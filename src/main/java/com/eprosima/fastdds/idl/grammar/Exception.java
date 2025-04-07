// Copyright 2025 Proyectos y Sistemas de Mantenimiento SL (eProsima).
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

package com.eprosima.fastdds.idl.grammar;

import com.eprosima.fastdds.idl.parser.typecode.StructTypeCode;

import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind;

import org.antlr.v4.runtime.Token;

public class Exception extends com.eprosima.idl.parser.tree.Exception
{
    public Exception(Context ctx, String scopeFile, boolean isInScope, String scope, String name, Token token)
    {
        super(scopeFile, isInScope, scope, name, token);
        m_context = ctx;
    }

    public StructTypeCode getTypeCode()
    {
        if (m_typecode == null)
        {
            m_typecode = new StructTypeCode(getScope(), getName());
            // Add inheritance
            m_typecode.addInheritance(m_context, getRpcOperationErrorTypeCode());
            // Add annotations
            getAnnotations().forEach((name, ann) -> m_typecode.addAnnotation(m_context, ann));
            // Add members
            getMembers().forEach(member -> m_typecode.addMember(member));
            // Default to final extensibility
            m_typecode.get_extensibility(ExtensibilityKind.FINAL);
        }

        return m_typecode;
    }

    private StructTypeCode getRpcOperationErrorTypeCode()
    {
        if (m_rpcOperationErrorTypeCode == null)
        {
            m_rpcOperationErrorTypeCode = new StructTypeCode("eprosima::fastdds::dds::rpc", "RpcOperationError");
            m_rpcOperationErrorTypeCode.get_extensibility(ExtensibilityKind.FINAL);
            int kind = com.eprosima.idl.parser.typecode.Kind.KIND_STRING;
            Member msg_member = new Member(m_context.createStringTypeCode(kind, null), "_error_message_");
            m_rpcOperationErrorTypeCode.addMember(msg_member);
        }
        return m_rpcOperationErrorTypeCode;
    }

    private Context m_context = null;
    private StructTypeCode m_typecode = null;
    static private StructTypeCode m_rpcOperationErrorTypeCode = null;
}
