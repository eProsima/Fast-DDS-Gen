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

    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;

        for (Bitfield member : getBitfields(true))
        {
            if (!member.isAnnotationNonSerialized())
            {
                current_alignment += ((TypeCode)member.getSpec().getTypecode()).maxSerializedSize(current_alignment);
            }
        }

        return current_alignment - initial_alignment;
    }

}
