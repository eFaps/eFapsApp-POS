package org.efaps.esjp.pos;

import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormPOS;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.uiform.Create;
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
            };
        }.execute(_parameter);
    }

    public enum Role
        implements IBitEnum
    {
        ADMIN,
        USER;

        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }
}
