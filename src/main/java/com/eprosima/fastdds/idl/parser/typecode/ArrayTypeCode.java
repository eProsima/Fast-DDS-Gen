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

public class ArrayTypeCode extends com.eprosima.idl.parser.typecode.ArrayTypeCode
    implements TypeCode
{

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;

        if (!getContentTypeCode().isPrimitive() &&
            !getContentTypeCode().isIsType_c() /*enum*/)
        {
            // DHEADER if XCDRv2
            current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4);
        }

        long size = 1;
        for (int count = 0; count < getEvaluatedDimensions().size(); ++count)
        {
            size *= Long.parseLong(getEvaluatedDimensions().get(count), 10);
        }

        if (0 < size)
        {
            current_alignment += ((TypeCode)getContentTypeCode()).maxSerializedSize(current_alignment);

            if (1 < size)
            {
                long element_size_after_first = ((TypeCode)getContentTypeCode()).maxSerializedSize(current_alignment);
                current_alignment += element_size_after_first * (size - 1);
            }
        }

        return current_alignment - initial_alignment;
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64) throws RuntimeGenerationException
    {
        long initial_alignment = current_alignment;

        long size = 1;
        for (int count = 0; count < getEvaluatedDimensions().size(); ++count)
        {
            size *= Long.parseLong(getEvaluatedDimensions().get(count), 10);
        }

        if (0 < size)
        {
            current_alignment += ((TypeCode)getContentTypeCode()).maxPlainTypeSerializedSize(
                    current_alignment, align64);

            if (1 < size)
            {
                long element_size_after_first = ((TypeCode)getContentTypeCode()).maxPlainTypeSerializedSize(
                        current_alignment, align64);
                current_alignment += element_size_after_first * (size - 1);
            }
        }

        return current_alignment - initial_alignment;
    }

    public boolean isNotZeroArray()
    {
        long size = 1;
        for (int count = 0; count < getEvaluatedDimensions().size(); ++count)
        {
            size *= Long.parseLong(getEvaluatedDimensions().get(count), 10);
        }
        return 0 != size;
    }
}
