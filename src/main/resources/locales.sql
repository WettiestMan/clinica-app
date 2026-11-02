CREATE COLLATION IF NOT EXISTS spanish_peru_ci_ai (
    PROVIDER = icu,
    LOCALE = 'es-PE-u-ks-level1',
    DETERMINISTIC = false
);

CREATE COLLATION IF NOT EXISTS bin (
    PROVIDER = icu,
    LOCALE = 'und'
);