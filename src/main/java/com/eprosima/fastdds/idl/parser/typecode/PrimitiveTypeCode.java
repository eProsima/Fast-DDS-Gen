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

public class PrimitiveTypeCode extends com.eprosima.idl.parser.typecode.PrimitiveTypeCode
    implements TypeCode
{

    public PrimitiveTypeCode(
            int kind)
    {
        super(kind);
    }

    public long getAlignmentAt()
    {
        switch (getKind())
        {
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGDOUBLE:
                return 16; // don't really know
            case com.eprosima.idl.parser.typecode.Kind.KIND_DOUBLE:
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGLONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONGLONG:
                return 8;
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_FLOAT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_WCHAR:
                return 4;
            case com.eprosima.idl.parser.typecode.Kind.KIND_SHORT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_USHORT:
                return 2;
            case com.eprosima.idl.parser.typecode.Kind.KIND_BOOLEAN:
            case com.eprosima.idl.parser.typecode.Kind.KIND_CHAR:
            case com.eprosima.idl.parser.typecode.Kind.KIND_OCTET:
            case com.eprosima.idl.parser.typecode.Kind.KIND_INT8:
            case com.eprosima.idl.parser.typecode.Kind.KIND_UINT8:
                return 1;
            default:
                System.out.println("Unexpected primitive type code kind: " + getKind());
                return 0;
        }
    }

    public long maxSerializedSize(
            long current_alignment)
    {
        long initial_alignment = current_alignment;

        System.out.println("maxSerializedSize primitive type code kind: " + getKind());

        switch (getKind())
        {
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGDOUBLE:
                current_alignment += 16 + TypeCode.cdr_alignment(current_alignment, 8);
                System.out.println("ALIGNMENT:  16/8");
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_DOUBLE:
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONGLONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONGLONG:
                current_alignment += 8 + TypeCode.cdr_alignment(current_alignment, 8);
                System.out.println("ALIGNMENT:  8/8");
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_LONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_ULONG:
            case com.eprosima.idl.parser.typecode.Kind.KIND_FLOAT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_WCHAR:
                current_alignment += 4 + TypeCode.cdr_alignment(current_alignment, 4);
                System.out.println("ALIGNMENT:  4/4");
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_SHORT:
            case com.eprosima.idl.parser.typecode.Kind.KIND_USHORT:
                current_alignment += 2 + TypeCode.cdr_alignment(current_alignment, 2);
                System.out.println("ALIGNMENT:  2/2");
                break;
            case com.eprosima.idl.parser.typecode.Kind.KIND_BOOLEAN:
            case com.eprosima.idl.parser.typecode.Kind.KIND_CHAR:
            case com.eprosima.idl.parser.typecode.Kind.KIND_OCTET:
            case com.eprosima.idl.parser.typecode.Kind.KIND_INT8:
            case com.eprosima.idl.parser.typecode.Kind.KIND_UINT8:
                System.out.println("ALIGNMENT:  1/");
                current_alignment += 1;
                break;
            default:
                System.out.println("Unexpected primitive type code kind: " + getKind());
        }

        return current_alignment - initial_alignment;
    }

}
