// Copyright 2023 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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

public interface MemberedTypeCode extends TypeCode
{
    static long xcdr_extra_header_serialized_size(
            long current_alignment,
            com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind memberedtypecode_ext_kind)
    {
        long returned_alignment = current_alignment;

        if (com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind.FINAL.get_value() < memberedtypecode_ext_kind.get_value())
        {
            // For APPENDABLE and MUTABLE, the maximum is the XCDR2 header (DHEADER(0) : Int32).
            returned_alignment += 4 + TypeCode.cdr_alignment(returned_alignment, 4);
        }

        return returned_alignment;
    }

    static long xcdr_extra_endheader_serialized_size(
            long current_alignment,
            com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind memberedtypecode_ext_kind)
    {
        long returned_alignment = current_alignment;

        if (com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind.MUTABLE.get_value() == memberedtypecode_ext_kind.get_value())
        {
            // For MUTABLE, extra alignment for the PID_SENTINAL.
            returned_alignment += TypeCode.cdr_alignment(returned_alignment, 4);
        }

        return returned_alignment;
    }

    static long xcdr_extra_member_serialized_size(
            long current_alignment,
            com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind memberedtypecode_ext_kind,
            boolean member_optional,
            Member member)
    {
        long returned_alignment = current_alignment;
        int member_size = member.getTypecode() instanceof PrimitiveTypeCode ? Integer.parseInt(((PrimitiveTypeCode)member.getTypecode()).getSize()) : 8;

        if (com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind.MUTABLE.get_value() == memberedtypecode_ext_kind.get_value() ||
                member_optional)
        {
            if (5 > member_size && 16384 /*2^14*/ >= member.get_id())
            {
                returned_alignment += 4 + TypeCode.cdr_alignment(returned_alignment, 4);
            }
            else
            {
                // If member is from a MUTABLE type (or it is optional member) the maximum is XCDR1 LongMemberHeader.
                // << ALIGN(4)
                // << { FLAG_I + FLAG_M + PID_EXTENDED : UInt16 }
                // << { slength=8 : UInt16 }
                // << { M.id : <<: UInt32 }
                // << { M.value.ssize : UInt32 }
                returned_alignment += 4 + 4 + 4 + TypeCode.cdr_alignment(returned_alignment, 4);
            }
        }

        return returned_alignment;
    }
}
