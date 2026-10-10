# Loan Decision Engine

A small loan underwriting system. An application goes through a policy rules engine and a
logistic regression risk model, an LLM writes a short explanation for the reviewer, and then
two different people (an analyst and a senior reviewer) have to sign off before it is final.

I built it to practise the parts of a lending system that tutorials usually skip: rules you can
change without redeploying, being able to say why a score came out the way it did, four-eyes
approval, and a history nobody can quietly edit.

Stack: Java 17, Spring Boot 3, Spring Security (JWT), JPA, PostgreSQL, Flyway, React (Vite), Recharts, Docker.

## What it does

- **Rules in the database.** Things like "debt to income above 40 percent means refer" are rows in
  `lending_rule`. A senior reviewer can change a limit on the Rules page and the next application
  uses it. Each change bumps a version number that gets stored on every decision.
- **Risk model.** Logistic regression trained in Python (`ml/train_model.py`), exported to JSON and
  scored inside the Java service, so there is no second service to run. Because the inputs are
  standardised, `coef * z` is each feature's exact contribution to the log odds, which gives the
  reason codes without needing SHAP.
- **LLM explanation.** The structured result (rules fired, top factors, probability) is sent to the
  Claude API with instructions to use only those facts. If there is no API key or the call fails,
  a fixed template is used instead and the page says which one you are reading.
- **Maker and checker.** An analyst recommends, a senior approves or declines. The same person
  cannot do both on one application.
- **Audit trail.** Every action is written to `audit_log`. Rows are chained by hash and a database
  trigger blocks update and delete. `GET /api/audit/verify` walks the chain and tells you if
  anything was changed.
- **What-if simulator.** Change income, debt or credit score and see the score and rules update live.

## How a request flows

```
React  ->  Spring Boot  ->  RuleEngine  (rules from Postgres)
                       ->  RiskModel    (model.json)
                       ->  ExplanationService  ->  Claude API (optional)
                       ->  Postgres  (application, decision, audit_log)
```

## Run it with Docker

```bash
cp .env.example .env        # add ANTHROPIC_API_KEY if you have one
docker compose up --build
```

Open http://localhost:3000

| user     | password    | role    |
|----------|-------------|---------|
| analyst  | analyst123  | ANALYST |
| analyst2 | analyst123  | ANALYST |
| senior   | senior123   | SENIOR  |

Try this: log in as `analyst`, create an application, send a recommendation, sign out, log in as
`senior`, approve it, then open the Rules page and press "Verify audit chain".

## Run it without Docker

```bash
# database
docker run -d --name loans-db -e POSTGRES_DB=loans -e POSTGRES_USER=loans \
  -e POSTGRES_PASSWORD=loans -p 5432:5432 postgres:16

# backend (needs Java 17 and Maven)
cd backend
mvn spring-boot:run

# frontend (needs Node 18+)
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /api to :8080
```

## Training your own model

```bash
cd ml
pip install -r requirements.txt
python train_model.py                  # sample data, just to see the flow
python train_model.py --csv mydata.csv # real data, see the docstring for columns
```

The script overwrites `backend/src/main/resources/model/model.json`. The model that ships in the
repo has hand-set coefficients so the app runs straight away, it is not trained on real lending
data. Retrain on LendingClub or German Credit before quoting any AUC anywhere.

## API

| method | path                                  | who              |
|--------|---------------------------------------|------------------|
| POST   | /api/auth/login                       | anyone           |
| GET    | /api/applications                     | any signed in    |
| POST   | /api/applications                     | analyst, senior  |
| GET    | /api/applications/{id}                | any signed in    |
| POST   | /api/applications/{id}/recommend      | analyst          |
| POST   | /api/applications/{id}/finalize       | senior           |
| POST   | /api/simulate                         | any signed in    |
| GET    | /api/rules                            | any signed in    |
| PUT    | /api/rules/{id}                       | senior           |
| GET    | /api/stats                            | any signed in    |
| GET    | /api/audit/verify                     | any signed in    |

## Tests

```bash
cd backend && mvn test
```

Unit tests cover the rule comparisons and the model (risky profiles must score higher than safe ones).

## Things I would do next

- Fairness checks. The model only uses financial features, but nothing proves it is fair across groups.
- Move the LLM call to a queue so a slow API never slows down submitting an application.
- Proper user management. The three demo users are created at startup by `DataSeeder`.
- Refresh tokens. Right now the JWT just expires after 8 hours.
- The explanation is not checked automatically against the facts. A second pass that compares numbers in the text with the JSON would catch mistakes.

## Project layout

```
backend/    Spring Boot API, migrations, tests
frontend/   React app
ml/         model training script
```
