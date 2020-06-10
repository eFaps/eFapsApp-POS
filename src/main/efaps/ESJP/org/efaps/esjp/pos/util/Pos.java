/*
 * Copyright 2003 - 2018 The eFaps Team
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
 *
 */

package org.efaps.esjp.pos.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSysConfLink;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.esjp.ci.CINumGenPOS;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.util.cache.CacheReloadException;

@EFapsUUID("77a6fc1e-a406-419f-a1aa-8207255a2522")
@EFapsApplication("eFapsApp-POS")
@EFapsSystemConfiguration("b038bf69-b588-431d-8c02-f53d4aac46c9")
public class Pos
{
    /** The base. */
    public static final String BASE = "org.efaps.pos.";
    /** POS-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("b038bf69-b588-431d-8c02-f53d4aac46c9");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Activate")
                    .description("Activate the POS implementation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute VERSION = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Version")
                    .defaultValue("0.0.0")
                    .description("Definition of the Version to be used local.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ALLOWAUTOIDENT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AllowBackendAutoIdent")
                    .description("Allow the generation of automatic Backend Identifier");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CATEGORY_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Category.Activate")
                    .description("Activate the handling of categories.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CATEGORY_ACIVATEIMAGE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Category.ActivateImage")
                    .description("Activate the image mechanism for categories.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FLOOR_ACIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Floor.Activate")
                    .description("Activate the handling for spots.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INDICATIONSET_ACIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IndicationSet.Activate")
                    .description("Activate the handling for spots.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENT_SEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PaymentDocument.Sequence")
                    .defaultValue(CINumGenPOS.PaymentDocumentSequence.uuid.toString())
                    .description("UUID of the Sequence used to AutoNumber the created Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Configurations")
                    .description("Configurations that will be forwarded to the offline POS.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODREL = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductRelations")
                    .addDefaultValue("Select", "linkfrom[Products_AlternativeBOM#From].linkto[To].oid")
                    .addDefaultValue("Label", "Puede ser replacado por")
                    .addDefaultValue("Select01", "linkfrom[Products_AlternativeBOM#To].linkto[From].oid")
                    .addDefaultValue("Label01", "Puede replacadar")
                    .description("Configurations of product relations to be included.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute BALANCE_REPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "BalanceReport")
                    .addDefaultValue("Type", CIPOS.Balance.getType().getName())
                    .description("Configurations for Balance Report");

    @EFapsSysConfLink
    public static final SysConfLink PRODDOCTYPE4DOC = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductDocumentType4TransactionDocumentShadow")
                    .description("ProductDocumentType to be used for registration of TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CONTACT_ACIVATEEMAIL = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Contact.ActivateEmail")
                    .description("Activate the handling of categories.");


    public enum Role implements IBitEnum
    {
        ADMIN, USER;

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

    public enum DocType implements IBitEnum
    {
        RECEIPT, INVOICE, TICKET;

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

    public enum SpotConfig implements IEnum
    {
        NONE, BASIC, EXTENDED;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    public enum PrinterType implements IEnum
    {
        PREVIEW, PHYSICAL;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    public enum PrintTarget implements IEnum
    {
        JOB,
        PRELIMINARY,
        TICKET,
        COPY,
        BALANCE;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    public enum PosLayout implements IEnum
    {
        GRID,
        LIST,
        BOTH;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    public enum GridSize implements IEnum
    {
        SMALL,
        MEDIUM,
        LARGE;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }


    public enum DiscountType implements IEnum
    {
        PERCENT,
        AMOUNT;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * @return the SystemConfigruation for Payroll
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // POS-Configuration
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
