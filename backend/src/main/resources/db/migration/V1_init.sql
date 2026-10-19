create table app_user (
    id            bigserial primary key,
    username      varchar(60) not null unique,
    password_hash varchar(100) not null,
    role          varchar(20) not null
);

create table loan_application (
    id               bigserial primary key,
    applicant_name   varchar(120) not null,
    age              integer not null,
    annual_income    numeric(14,2) not null,
    monthly_debt     numeric(14,2) not null,
    loan_amount      numeric(14,2) not null,
    term_months      integer not null,
    credit_score     integer not null,
    delinquencies    integer not null,
    employment_years numeric(4,1) not null,
    status           varchar(24) not null,
    created_by       varchar(60) not null,
    created_at       timestamptz not null default now()
);

create table lending_rule (
    id        bigserial primary key,
    name      varchar(120) not null,
    field     varchar(40) not null,
    operator  varchar(4) not null,
    threshold numeric(14,4) not null,
    outcome   varchar(10) not null,
    message   varchar(200) not null,
    active    boolean not null default true,
    version   integer not null default 1
);

create table decision (
    id                    bigserial primary key,
    application_id        bigint not null unique references loan_application(id),
    system_recommendation varchar(10) not null,
    risk_score            double precision not null,
    model_version         varchar(40) not null,
    rules_version         varchar(40) not null,
    rule_hits             text not null,
    reason_codes          text not null,
    explanation           text,
    explanation_source    varchar(20),
    analyst_recommendation varchar(10),
    analyst_note          text,
    recommended_by        varchar(60),
    recommended_at        timestamptz,
    final_decision        varchar(10),
    final_note            text,
    decided_by            varchar(60),
    decided_at            timestamptz,
    created_at            timestamptz not null default now()
);

create table audit_log (
    id          bigserial primary key,
    entity_type varchar(30) not null,
    entity_id   bigint not null,
    action      varchar(40) not null,
    actor       varchar(60) not null,
    details     text,
    prev_hash   varchar(64),
    hash        varchar(64) not null,
    created_at  timestamptz not null
);

-- the audit log can only be appended to
create function audit_log_readonly() returns trigger as $$
begin
    raise exception 'audit_log is append only';
end;
$$ language plpgsql;

create trigger audit_log_no_update
    before update or delete on audit_log
    for each row execute function audit_log_readonly();
