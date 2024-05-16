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

import com.eprosima.idl.parser.typecode.Bitfield;

public class BitsetTypeCode extends com.eprosima.idl.parser.typecode.BitsetTypeCode
    implements TypeCode
{

    public BitsetTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        return maxPlainTypeSerializedSize(current_alignment, 8);
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64)
    {
        long initial_alignment = current_alignment;

        int full_bit_size = getBitSize();

        if (9 > full_bit_size)
        {
            current_alignment += 1;
        }
        else if (17 > full_bit_size)
        {
            current_alignment += 2 + TypeCode.cdr_alignment(current_alignment, 2);
        }
        else if (33 > full_bit_size)
        {
            current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4);
        }
        else
        {
            current_alignment += 8 + TypeCode.cdr_alignment(current_alignment, align64);
        }

        return current_alignment - initial_alignment;
    }

}
