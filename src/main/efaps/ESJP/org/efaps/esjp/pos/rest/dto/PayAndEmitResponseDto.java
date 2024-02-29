package org.efaps.esjp.pos.rest.dto;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.AbstractPayableDocumentDto;
import org.efaps.pos.dto.DocType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = PayAndEmitResponseDto.Builder.class)
@EFapsUUID("abf31062-ecbe-43d2-91e3-652669d53d6f")
@EFapsApplication("eFapsApp-POS")
public class PayAndEmitResponseDto
{

    private final DocType payableType;

    private final AbstractPayableDocumentDto payable;

    private PayAndEmitResponseDto(Builder builder)
    {
        this.payableType = builder.payableType;
        this.payable = builder.payable;
    }

    public DocType getPayableType()
    {
        return payableType;
    }

    public AbstractPayableDocumentDto getPayable()
    {
        return payable;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private DocType payableType;
        private AbstractPayableDocumentDto payable;

        private Builder()
        {
        }

        public Builder withPayableType(DocType payableType)
        {
            this.payableType = payableType;
            return this;
        }

        public Builder withPayable(AbstractPayableDocumentDto payable)
        {
            this.payable = payable;
            return this;
        }

        public PayAndEmitResponseDto build()
        {
            return new PayAndEmitResponseDto(this);
        }
    }
}
