// Copyright 2021 Proyectos y Sistemas de Mantenimiento SL (eProsima).
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.eprosima.fastdds.idl.parser.typecode;

import com.eprosima.idl.parser.exception.RuntimeGenerationException;

public class AliasTypeCode extends com.eprosima.idl.parser.typecode.AliasTypeCode
    implements TypeCode
{
    public AliasTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    public boolean isHasKey()
    {
        boolean returnedValue = false;

        if (getContentTypeCode() instanceof StructTypeCode)
        {
            returnedValue = ((StructTypeCode)getContentTypeCode()).isHasKey();
        }

        return returnedValue;
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        return ((TypeCode) getTypedefContentTypeCode()).maxSerializedSize(current_alignment);
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64) throws RuntimeGenerationException
    {
        return ((TypeCode) getTypedefContentTypeCode()).maxPlainTypeSerializedSize(current_alignment, align64);
    }

    public boolean isNotZeroArray()
    {
        if (super.getContentTypeCode() instanceof ArrayTypeCode)
        {
            return ((ArrayTypeCode) super.getContentTypeCode()).isNotZeroArray();
        }
        return true;
    }
}
