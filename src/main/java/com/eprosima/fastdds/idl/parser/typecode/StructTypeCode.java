// Copyright 2016 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.tree.Annotation;

import java.util.ArrayList;

public class StructTypeCode extends com.eprosima.idl.parser.typecode.StructTypeCode
    implements MemberedTypeCode
{
    public StructTypeCode(
            String scope,
            String name)
    {
        super(scope, name);
    }

    public boolean isHasKey()
    {
        boolean returnedValue = false;

        if (getEnclosedInheritance() != null)
        {
            returnedValue |= ((StructTypeCode)getEnclosedInheritance()).isHasKey();
        }

        for (int count = 0; count < getMembers().size() && !returnedValue; ++count)
        {
            Member member = getMembers().get(count);
            returnedValue = member.isAnnotationKey();
        }

        return returnedValue;
    }

    public String getMaxSerializedSize()
    {
        return Long.toString(maxSerializedSize(0, false, get_extensibility()));
    }

    @Override
    public long maxSerializedSize(
            long current_alignment)
    {
        return maxSerializedSize(current_alignment, false, get_extensibility());
    }


    private long maxSerializedSize(
            long current_alignment,
            boolean only_keys,
            com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind struct_ext_kind)
    {
        long initial_alignment = current_alignment;

        if (!detect_recursive_)
        {
            detect_recursive_ = true;
            current_alignment = MemberedTypeCode.xcdr_extra_header_serialized_size(current_alignment, struct_ext_kind);

            // TODO if only_key, get members sorted.
            for (Member member : getAllMembers())
            {
                if (member.isAnnotationNonSerialized())
                {
                    continue;
                }
                if (only_keys && isHasKey())
                {
                    if (member.isAnnotationKey())
                    {
                        if (member.getTypecode() instanceof StructTypeCode &&
                                ((StructTypeCode)member.getTypecode()).isHasKey())
                        {
                            current_alignment +=
                                ((StructTypeCode)member.getTypecode()).maxSerializedSize(current_alignment, true,
                                com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind.FINAL); // FINAL to avoid calculation
                                                                                                    // of any XCDR header.
                        }
                        else
                        {
                            current_alignment += ((TypeCode)member.getTypecode()).maxSerializedSize(current_alignment);
                        }
                    }
                }
                else if (!only_keys)
                {
                    current_alignment = MemberedTypeCode.xcdr_extra_member_serialized_size(
                            current_alignment,
                            struct_ext_kind, member.isAnnotationOptional(),
                            member);
                    current_alignment += ((TypeCode)member.getTypecode()).maxSerializedSize(current_alignment);
                }
            }

            current_alignment = MemberedTypeCode.xcdr_extra_endheader_serialized_size(current_alignment, struct_ext_kind);
            detect_recursive_ = false;
        }

        return current_alignment - initial_alignment;
    }

    public String getMaxKeySerializedSize()
    {
        return Long.toString(maxSerializedSize(0, true,
                    com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind.FINAL));
    }

    public String getMaxXCDRv1PlainTypeSerializedSize() throws RuntimeGenerationException
    {
        return Long.toString(maxPlainTypeSerializedSize(0, 8));
    }

    public String getMaxXCDRv2PlainTypeSerializedSize() throws RuntimeGenerationException
    {
        return Long.toString(maxPlainTypeSerializedSize(0, 4));
    }

    @Override
    public long maxPlainTypeSerializedSize(
            long current_alignment,
            long align64) throws RuntimeGenerationException
    {
        if (ExtensibilityKind.FINAL != get_extensibility())
        {
            throw new RuntimeGenerationException("StructTypeCode::maxPlainTypeSerializedSize(): only FINAL structures can be plain.");
        }

        long initial_alignment = current_alignment;

        if (getEnclosedInheritance() != null)
        {
            current_alignment += ((StructTypeCode)getEnclosedInheritance()).maxPlainTypeSerializedSize(current_alignment, align64);
        }

        for (Member member : getMembers())
        {
            if (member.isAnnotationNonSerialized())
            {
                continue;
            }

            if (member.isIsPlain())
            {
                current_alignment += ((TypeCode)member.getTypecode()).maxPlainTypeSerializedSize(current_alignment, align64);
            }
            else
            {
                throw new RuntimeGenerationException("StructTypeCode::maxPlainTypeSerializedSize(): A member returned being non-plain.");
            }
        }

        return current_alignment - initial_alignment;
    }

    public void setIsTopic(
            boolean value)
    {
        istopic_ = value;
    }

    public boolean isIsTopic()
    {
        return istopic_;
    }

    public ArrayList<String> getNamespaces()
    {
        ArrayList<String> namespaces = new ArrayList<String>();
        String scopes = getScope();
        int ch_pos = scopes.indexOf("::");

        while (0 < ch_pos)
        {
            namespaces.add(scopes.substring(0, ch_pos));
            scopes = scopes.substring(ch_pos + 2);
            ch_pos = scopes.indexOf("::");
        }

        if (!scopes.isEmpty())
        {
            namespaces.add(scopes);
        }

        return namespaces;
    }

    private boolean istopic_ = true;
}
