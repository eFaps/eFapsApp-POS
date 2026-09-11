--
-- Copyright © 2003 - 2024 The eFaps Team (-)
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

DO $$
DECLARE
    row_record RECORD;
BEGIN
    FOR row_record IN
        SELECT
            CM.ID
        FROM
            T_CMABSTRACT CM
            INNER JOIN T_DMATTRIBUTE DM ON DM.ID = CM.ID
        WHERE
            TYPEID IN (
                SELECT
                    ID
                FROM
                    T_CMABSTRACT
                WHERE
                    NAME = 'Admin_DataModel_AttributeSet'
            )
            AND NAME = 'TagSet'
            AND DMTYPE IN (
                SELECT
                    ID
                FROM
                    T_CMABSTRACT
                WHERE
                    NAME = 'POS_File'
            )
    LOOP
        RAISE NOTICE 'Processing ID: %', row_record.id;

        DELETE from T_DMATTRIBUTE where id = row_record.id;
        DELETE FROM T_CMABSTRACT where id = row_record.id;
    END LOOP;
END $$;


UPDATE T_POSATTR AS PA
SET
    TYPEID = SUBQUERY.ID
FROM
    (
        SELECT
            CM.ID
        FROM
            T_CMABSTRACT CM
            INNER JOIN T_DMATTRIBUTE DM ON DM.ID = CM.ID
        WHERE
            TYPEID IN (
                SELECT
                    ID
                FROM
                    T_CMABSTRACT
                WHERE
                    NAME = 'Admin_DataModel_AttributeSet'
            )
            AND NAME = 'TagSet'
            AND DMTYPE IN (
                SELECT
                    ID
                FROM
                    T_CMABSTRACT
                WHERE
                    NAME = 'POS_FileAbstract'
            )
    ) AS SUBQUERY
WHERE
    PA.TYPEID NOT IN (
        SELECT
            CM.ID
        FROM
            T_CMABSTRACT CM
    );
