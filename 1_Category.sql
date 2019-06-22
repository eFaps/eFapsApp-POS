ALTER TABLE t_poscat
    ADD COLUMN associd bigint;

UPDATE t_poscat
SET associd = t_cmassocdef.associd
FROM t_poscat tp
LEFT JOIN t_cmassocdef ON t_cmassocdef.companyid = tp.companyid;

ALTER TABLE t_poscat
    DROP COLUMN companyid;
