package org.efaps.esjp.pos.rest;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e8b712b5-bb83-4528-9513-aac2c970fcc0")
@EFapsApplication("eFapsApp-POS")
public class ForbiddenException
    extends ForbiddenException_Base
{
    /** */
    private static final long serialVersionUID = 1L;

    public ForbiddenException(final String _message) {
        super(_message);
    }
}
