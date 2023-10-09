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

public class SetTypeCode extends com.eprosima.idl.parser.typecode.SetTypeCode
    implements TypeCode
{
    public SetTypeCode(
            String maxsize,
            String evaluated_maxsize)
    {
        super(maxsize, evaluated_maxsize);
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;
        long maxsize = Long.parseLong(getEvaluatedMaxsize(), 10);

        current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4);

        if (0 < maxsize)
        {
            current_alignment += ((TypeCode)getContentTypeCode()).maxSerializedSize(current_alignment);

            if (1 < maxsize)
            {
                long element_size_after_first = ((TypeCode)getContentTypeCode()).maxSerializedSize(current_alignment);
                current_alignment += element_size_after_first * (maxsize - 1);
            }
        }

        return current_alignment - initial_alignment;
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64) throws RuntimeGenerationException
    {
        throw new RuntimeGenerationException("MapTypeCode::maxPlainTypeSerializedSize(): Sets are not plain types.");
    }

}
