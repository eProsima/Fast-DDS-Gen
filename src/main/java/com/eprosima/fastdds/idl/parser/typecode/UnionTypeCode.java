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

import com.eprosima.idl.parser.typecode.Member;

public class UnionTypeCode extends com.eprosima.idl.parser.typecode.UnionTypeCode
    implements TypeCode
{
    public UnionTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    public UnionTypeCode(
            String scope,
            String name,
            com.eprosima.idl.parser.typecode.TypeCode discriminatorTypeCode)
    {
        super(scope, name, discriminatorTypeCode);
    }

    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;
        long reset_alignment = 0;
        long union_max_size_serialized = 0;

        current_alignment += ((TypeCode)getDiscriminator()).maxSerializedSize(current_alignment);

        for (Member member : getMembers())
        {
            reset_alignment = current_alignment;
            reset_alignment += ((TypeCode)member.getTypecode()).maxSerializedSize(reset_alignment);
            if (union_max_size_serialized < reset_alignment)
            {
                union_max_size_serialized = reset_alignment;
            }
        }

        return union_max_size_serialized - initial_alignment;
    }

}
