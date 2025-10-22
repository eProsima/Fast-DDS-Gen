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

import com.eprosima.fastdds.idl.grammar.Operation;
import com.eprosima.fastdds.idl.parser.typecode.EnumTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.StructTypeCode;
import com.eprosima.idl.parser.exception.ParseException;
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.typecode.EnumMember;
import com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind;
import org.antlr.v4.runtime.Token;

public class Interface extends com.eprosima.idl.parser.tree.Interface
{
    public Interface(Context ctx, String scopeFile, boolean isInScope, String scope, String name, Token tk)
    {
        super(scopeFile, isInScope, scope, name, tk);
        m_context = ctx;
    }

    @Override
    public void add(com.eprosima.idl.parser.tree.Export exp)
    {
        super.add(exp);

        if (exp instanceof Operation)
        {
            Operation op = (Operation)exp;
            if (op.getOutputparam().size() > 0)
            {
                m_has_operations_with_output_arguments = true;
            }

            m_context.operationAdded(op);
        }
    }

    /*!
     * @brief This function is used to check if the interface has output feeds.
     *
     * @return True if the interface has output feeds, false otherwise.
     */
    public boolean isWithOutputFeeds()
    {
        return m_hasOutputFeeds;
    }

    /*!
     * @ingroup api_for_stg
     *
     * @brief This function is used to check if the interface has operations with output arguments.
     *
     * @return True if the interface has operations with output arguments, false otherwise.
     */
    public boolean isWithOutputParameters()
    {
        return m_has_operations_with_output_arguments;
    }

    /*!
     * @brief This function is used in stringtemplates to generate the typesupport code for the interface.
     *
     * @return The typecode of the request type.
     */
    public StructTypeCode getRequestTypeCode()
    {
        if (m_request_type == null)
        {
            String scope = getHasScope() ? getScope() + "::detail" : "detail";
            StructTypeCode request_type = new StructTypeCode(scope, getName() + "_Request");

            // The request type should be a `@choice`, which means that it should have MUTABLE extensibility,
            // and also treat all fields as optional.
            request_type.get_extensibility(ExtensibilityKind.MUTABLE);

            getAll_operations().forEach(operation -> {
                Operation op = (Operation)operation;
                Member member = new Member(op.getInTypeCode(), op.getName());
                // Add `@optional` and `@hashid` annotations
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
                request_type.addMember(member);
                // Add input feed parameters
                op.getParameters().forEach(param -> {
                    Param p = (Param)param;
                    if (p.isInput() && p.isAnnotationFeed())
                    {
                        Member feed_member = new Member(p.getFeedTypeCode(), op.getName() + "_" + p.getName());
                        feed_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                        feed_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
                        request_type.addMember(feed_member);
                    }
                });
            });

            if (m_hasOutputFeeds)
            {
                // Optional boolean to indicate if the feed is cancelled
                Member feed_cancel = new Member(m_context.createPrimitiveTypeCode(
                    com.eprosima.idl.parser.typecode.Kind.KIND_BOOLEAN), "feed_cancel_");
                feed_cancel.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                feed_cancel.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
                request_type.addMember(feed_cancel);
            }

            m_request_type = request_type;
        }
        return m_request_type;
    }

    /*!
     * @brief This function is used in stringtemplates to generate the typesupport code for the interface.
     *
     * @return The typecode of the reply type.
     */
    public StructTypeCode getReplyTypeCode()
    {
        if (m_reply_type == null)
        {
            String scope = getHasScope() ? getScope() + "::detail" : "detail";
            StructTypeCode reply_type = new StructTypeCode(scope, getName() + "_Reply");

            // The reply type should be a `@choice`, which means that it should have MUTABLE extensibility,
            // and also treat all fields as optional.
            reply_type.get_extensibility(ExtensibilityKind.MUTABLE);

            getAll_operations().forEach(operation -> {
                Operation op = (Operation)operation;
                Member member = new Member(op.getResultTypeCode(), op.getName());
                // Add `@optional` and `@hashid` annotations
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
                reply_type.addMember(member);
            });

            Member remoteEx = new Member(get_remoteExceptionCode_t_type(), "remoteEx");
            remoteEx.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
            remoteEx.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
            reply_type.addMember(remoteEx);

            m_reply_type = reply_type;
        }
        return m_reply_type;
    }

    private EnumTypeCode get_remoteExceptionCode_t_type()
    {
        if (m_remoteExceptionCode_t_type == null)
        {
            EnumTypeCode type = new EnumTypeCode("eprosima::fastdds::dds::rpc", "RemoteExceptionCode_t");
            type.addMember(new EnumMember("REMOTE_EX_OK"));
            type.addMember(new EnumMember("REMOTE_EX_UNSUPPORTED"));
            type.addMember(new EnumMember("REMOTE_EX_INVALID_ARGUMENT"));
            type.addMember(new EnumMember("REMOTE_EX_OUT_OF_RESOURCES"));
            type.addMember(new EnumMember("REMOTE_EX_UNKNOWN_OPERATION"));
            type.addMember(new EnumMember("REMOTE_EX_UNKNOWN_EXCEPTION"));
            m_remoteExceptionCode_t_type = type;
        }
        return m_remoteExceptionCode_t_type;
    }

    private Context m_context = null;
    private boolean m_hasOutputFeeds = false;
    private boolean m_has_operations_with_output_arguments = false;
    private StructTypeCode m_request_type = null;
    private StructTypeCode m_reply_type = null;
    static private EnumTypeCode m_remoteExceptionCode_t_type = null;
}
