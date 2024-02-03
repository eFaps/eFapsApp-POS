/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.efaps.esjp.pos;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormPOS;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.util.EFapsException;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

@EFapsUUID("19ab3908-e01d-486f-8290-b2403593c5df")
@EFapsApplication("eFapsApp-POS")
public abstract class User_Base
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter, final Insert _insert)
                throws EFapsException
            {
                _insert.add(CIPOS.User.EmployeeLink, Instance.get(_parameter.getParameterValue(
                                CIFormPOS.POS_UserForm.employeeLink.name)));

                final String clearTextPwd = _parameter.getParameterValue(CIFormPOS.POS_UserForm.clearTextPwd.name);

                final PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                _insert.add(CIPOS.User.Password, passwordEncryptor.encryptPassword(clearTextPwd));
            }
        }.execute(_parameter);
    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        return new Edit()
        {

            @Override
            protected void add2MainUpdate(final Parameter _parameter, final Update _update)
                throws EFapsException
            {
                _update.add(CIPOS.User.EmployeeLink, Instance.get(_parameter.getParameterValue(
                                CIFormPOS.POS_UserForm.employeeLink.name)));
            }
        }.execute(_parameter);
    }

    public Return setPassword(final Parameter _parameter)
        throws EFapsException
    {
        return new Edit()
        {

            @Override
            protected void add2MainUpdate(final Parameter _parameter, final Update _update)
                throws EFapsException
            {
                final String clearTextPwd = _parameter.getParameterValue(CIFormPOS.POS_UserForm.clearTextPwd.name);

                final PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                _update.add(CIPOS.User.Password, passwordEncryptor.encryptPassword(clearTextPwd));
            }
        }.execute(_parameter);
    }

}
