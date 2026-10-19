insert into lending_rule (name, field, operator, threshold, outcome, message) values
 ('Very high debt to income',   'dti',              'GT',  50,   'DECLINE', 'Debt to income ratio is above 50 percent'),
 ('High debt to income',        'dti',              'GT',  40,   'REFER',   'Debt to income ratio is above 40 percent'),
 ('Very low credit score',      'credit_score',     'LT',  550,  'DECLINE', 'Credit score is below 550'),
 ('Low credit score',           'credit_score',     'LT',  620,  'REFER',   'Credit score is below 620'),
 ('Many recent delinquencies',  'delinquencies',    'GTE', 3,    'DECLINE', 'Three or more delinquencies in the last 2 years'),
 ('Large loan versus income',   'loan_to_income',   'GT',  0.6,  'REFER',   'Loan is more than 60 percent of annual income'),
 ('Applicant is a minor',       'age',              'LT',  18,   'DECLINE', 'Applicant is under 18'),
 ('Short employment history',   'employment_years', 'LT',  1,    'REFER',   'Less than one year at current employer');
