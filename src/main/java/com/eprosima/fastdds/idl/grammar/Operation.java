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
import com.eprosima.fastdds.idl.grammar.Param;
import com.eprosima.idl.parser.exception.RuntimeGenerationException;
import com.eprosima.idl.parser.exception.ParseException;
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.TypeCode.ExtensibilityKind;
import org.antlr.v4.runtime.Token;

public class Operation extends com.eprosima.idl.parser.tree.Operation
{
    public Operation(Context ctx, String scopeFile, boolean isInScope, String scope, String name, Token tk)
    {
        super(scopeFile, isInScope, scope, name, tk);
        m_context = ctx;
    }

    /**
     * Return whether the operation has been annotated as feed
     *
     * @return true when the operation has been annotated as feed, false otherwise.
     */
    public boolean isAnnotationFeed()
    {
        com.eprosima.idl.parser.tree.Annotation ann = getAnnotations().get(Annotation.rpc_feed_str);
        if (ann != null)
        {
            try
            {
                return ann.getValue().toUpperCase().equals(Annotation.capitalized_true_str);
            }
            catch (RuntimeGenerationException ex)
            {
                // Should not be called as @feed annotation has only one parameter
            }
        }
        return false;
    }

    /**
     * Return whether the operation has been annotated as mutable
     *
     * @return true when the operation has been annotated as mutable, false otherwise.
     */
    public boolean isAnnotationMutable()
    {
        // TODO(MiguelCompany): Add support for @mutable annotation
        return false;
    }

    public boolean isHasInputFeeds()
    {
        return false;
    }

    public StructTypeCode getInTypeCode()
    {
        if (m_in_type == null)
        {
            Interface parent = (Interface)getParent();
            String scope = parent.getHasScope() ? parent.getScope() + "::detail" : "detail";

            // Create In type
            StructTypeCode in_type = new StructTypeCode(
                scope,
                parent.getName() + "_" + getName() + "_In");

            // If the operation is marked as mutable, then the in type should be mutable as well
            if (isAnnotationMutable())
            {
                in_type.get_extensibility(ExtensibilityKind.MUTABLE);
            }
            else
            {
                in_type.get_extensibility(ExtensibilityKind.FINAL);
            }

            // Add non-feed input parameters as members
            getParameters().forEach(param -> {
                if (param.isInput() && !((Param)param).isAnnotationFeed())
                {
                    Member member = new Member(param.getTypecode(), param.getName());
                    in_type.addMember(member);
                }
            });

            m_in_type = in_type;
        }
        return m_in_type;
    }

    public StructTypeCode createFeedTypeCode(Param p)
    {
        // Add feed typecode to the parameter
        Interface iface = (Interface)getParent();
        String scope = iface.getHasScope() ? iface.getScope() + "::detail" : "detail";

        StructTypeCode feed_type = new StructTypeCode(
            scope,
            iface.getName() + "_" + getName() + "_" + p.getName() + "_Feed");

        feed_type.get_extensibility(ExtensibilityKind.FINAL);

        // Add optional value member
        Member value_member = new Member(p.getTypecode(), "value");
        value_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
        feed_type.addMember(value_member);

        // Add optional finished_ member
        Member finished_member = new Member(m_context.createPrimitiveTypeCode(Kind.KIND_LONG), "finished_");
        finished_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
        feed_type.addMember(finished_member);

        return feed_type;
    }

    public StructTypeCode getOutTypeCode()
    {
        if (m_out_type == null)
        {
            Interface parent = (Interface)getParent();
            String scope = parent.getHasScope() ? parent.getScope() + "::detail" : "detail";

            // Create Out type
            StructTypeCode out_type = new StructTypeCode(
                scope,
                parent.getName() + "_" + getName() + "_Out");

            // If the operation is marked as mutable, then the out type should be mutable as well
            if (isAnnotationMutable())
            {
                out_type.get_extensibility(ExtensibilityKind.MUTABLE);
            }
            else
            {
                out_type.get_extensibility(ExtensibilityKind.FINAL);
            }

            if (isAnnotationFeed())
            {
                // Feeds have an optional return_ member, and an optional finished_ member
                Member return_member = new Member(getRettype(), "return_");
                return_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                out_type.addMember(return_member);
                Member finished_member = new Member(m_context.createPrimitiveTypeCode(Kind.KIND_BOOLEAN), "finished_");
                finished_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                out_type.addMember(finished_member);
            }
            else
            {
                // Add output parameters as members
                getParameters().forEach(param -> {
                    if (param.isOutput())
                    {
                        Member member = new Member(param.getTypecode(), param.getName());
                        out_type.addMember(member);
                    }
                });
                // Add return_ member if operation has a return type
                if (getRettype() != null)
                {
                    Member return_member = new Member(getRettype(), "return_");
                    out_type.addMember(return_member);
                }
            }

            m_out_type = out_type;
        }

        return m_out_type;
    }

    public StructTypeCode getResultTypeCode()
    {
        if (m_result_type == null)
        {
            Interface parent = (Interface)getParent();
            String scope = parent.getHasScope() ? parent.getScope() + "::detail" : "detail";
            StructTypeCode result_type = new StructTypeCode(
                scope,
                parent.getName() + "_" + getName() + "_Result");

            // The result type should be a `@choice`, which means that it should have MUTABLE extensibility
            result_type.get_extensibility(ExtensibilityKind.MUTABLE);

            // Add out type as a member of the result type
            Member result_member = new Member(getOutTypeCode(), "result");
            result_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
            result_member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
            result_type.addMember(result_member);

            // Add exceptions as optional members
            getExceptions().forEach(exception -> {
                String member_name = exception.getFormatedScopedname() + "_ex";
                Member member = new Member(((Exception)exception).getTypeCode(), member_name);
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("optional")));
                member.addAnnotation(m_context, new Annotation(m_context.getAnnotationDeclaration("hashid")));
                result_type.addMember(member);
            });

            m_result_type = result_type;
        }

        return m_result_type;
    }

    private Context m_context = null;
    private StructTypeCode m_in_type = null;
    private StructTypeCode m_out_type = null;
    private StructTypeCode m_result_type = null;
}
