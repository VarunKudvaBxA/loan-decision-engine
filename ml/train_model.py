"""
Trains the logistic regression used by the backend and writes it to
backend/src/main/resources/model/model.json

Usage:
    python train_model.py                     # trains on generated sample data
    python train_model.py --csv loans.csv     # trains on your own data

The csv needs these columns (one row per loan):
    dti, credit_score, delinquencies, loan_to_income, employment_years, term_months, default
where default is 1 if the borrower defaulted and 0 otherwise.
For LendingClub or German Credit you will need to build those columns first,
the java side computes them the same way (see Metrics.java).
"""
import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

FEATURES = [
    ("dti", "Debt to income ratio"),
    ("credit_score", "Credit score"),
    ("delinquencies", "Recent delinquencies"),
    ("loan_to_income", "Loan to income"),
    ("employment_years", "Years employed"),
    ("term_months", "Loan term"),
]

OUT = Path(__file__).resolve().parents[1] / "backend/src/main/resources/model/model.json"


def make_sample_data(n=20000, seed=7):
    rng = np.random.default_rng(seed)
    df = pd.DataFrame({
        "dti": np.clip(rng.normal(38, 14, n), 3, 90),
        "credit_score": np.clip(rng.normal(680, 70, n), 400, 850),
        "delinquencies": rng.poisson(0.5, n),
        "loan_to_income": np.clip(rng.normal(0.35, 0.25, n), 0.02, 2.0),
        "employment_years": np.clip(rng.normal(5, 4, n), 0, 35),
        "term_months": rng.choice([12, 24, 36, 48, 60, 72], n),
    })
    logit = (-1.7
             + 0.045 * (df.dti - 38)
             - 0.012 * (df.credit_score - 680)
             + 0.55 * df.delinquencies
             + 1.2 * (df.loan_to_income - 0.35)
             - 0.07 * (df.employment_years - 5)
             + 0.012 * (df.term_months - 42))
    p = 1 / (1 + np.exp(-logit))
    df["default"] = rng.binomial(1, p)
    return df


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", help="path to training data")
    args = ap.parse_args()

    df = pd.read_csv(args.csv) if args.csv else make_sample_data()
    cols = [f for f, _ in FEATURES]
    X, y = df[cols], df["default"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=1, stratify=y)

    scaler = StandardScaler().fit(X_train)
    clf = LogisticRegression(max_iter=1000, class_weight=None)
    clf.fit(scaler.transform(X_train), y_train)

    auc = roc_auc_score(y_test, clf.predict_proba(scaler.transform(X_test))[:, 1])
    print(f"test AUC: {auc:.3f} on {len(X_test)} rows")

    spec = {
        "version": f"logreg-{'custom' if args.csv else 'sample'}-auc{auc:.2f}",
        "intercept": float(clf.intercept_[0]),
        "features": [
            {
                "name": name,
                "label": label,
                "mean": float(scaler.mean_[i]),
                "std": float(scaler.scale_[i]),
                "coef": float(clf.coef_[0][i]),
            }
            for i, (name, label) in enumerate(FEATURES)
        ],
    }
    OUT.write_text(json.dumps(spec, indent=2))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
