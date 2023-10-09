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

package com.eprosima.fastdds.idl.grammar;

import com.eprosima.fastdds.idl.parser.typecode.AliasTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.ArrayTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.BitmaskTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.BitsetTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.EnumTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.MapTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.SequenceTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.SetTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.StringTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.StructTypeCode;
import com.eprosima.fastdds.idl.parser.typecode.UnionTypeCode;
import com.eprosima.idl.parser.tree.Annotation;
import com.eprosima.idl.parser.tree.Interface;
import com.eprosima.idl.parser.tree.TypeDeclaration;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.TypeCode;
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.typecode.MemberedTypeCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.Stack;


public class Context extends com.eprosima.idl.context.Context implements com.eprosima.fastcdr.idl.context.Context
{
    // TODO Remove middleware parameter. It is temporal while cdr and rest don't have async functions.
    public Context(
            String file,
            ArrayList<String> includePaths,
            boolean subscribercode,
            boolean publishercode,
            String appProduct,
            boolean generate_type_object,
            boolean generate_typesc,
            boolean generate_type_ros2)
    {
        super(file, includePaths);
        m_fileNameUpper = getFilename().toUpperCase();
        m_subscribercode = subscribercode;
        m_publishercode = publishercode;
        m_randomGenNames = new Stack<String>();

        // TODO Remove
        m_appProduct = appProduct;
        //m_protocol = protocol;
        //m_ddstypes = ddstypes;

        m_type_object = generate_type_object;
        m_typesc = generate_typesc;
        m_type_ros2 = generate_type_ros2;
    }

    public void setTypelimitation(
            String lt)
    {
        m_typelimitation = lt;
    }

    public String getTypelimitation()
    {
        return m_typelimitation;
    }

    @Override
    public AliasTypeCode createAliasTypeCode(
            String scope,
            String name)
    {
        return new AliasTypeCode(scope, name);
    }

    @Override
    public ArrayTypeCode createArrayTypeCode()
    {
        return new ArrayTypeCode();
    }

    @Override
    public BitsetTypeCode createBitsetTypeCode(
            String scope,
            String name)
    {
        return new BitsetTypeCode(scope, name);
    }

    @Override
    public BitmaskTypeCode createBitmaskTypeCode(
            String scope,
            String name)
    {
        return new BitmaskTypeCode(scope, name);
    }

    @Override
    public EnumTypeCode createEnumTypeCode(
            String scope,
            String name)
    {
        return new EnumTypeCode(scope, name);
    }

    @Override
    public MapTypeCode createMapTypeCode(
            String maxsize)
    {
        return new MapTypeCode(maxsize, evaluate_literal(maxsize));
    }

    @Override
    public PrimitiveTypeCode createPrimitiveTypeCode(
            int kind)
    {
        return new PrimitiveTypeCode(kind);
    }

    @Override
    public SequenceTypeCode createSequenceTypeCode(
            String maxsize)
    {
        return new SequenceTypeCode(maxsize, evaluate_literal(maxsize));
    }

    @Override
    public SetTypeCode createSetTypeCode(
            String maxsize)
    {
        return new SetTypeCode(maxsize, evaluate_literal(maxsize));
    }

    @Override
    public StringTypeCode createStringTypeCode(
            int kind,
            String maxsize)
    {
        return new StringTypeCode(kind, maxsize, evaluate_literal(maxsize));
    }

    @Override
    public StructTypeCode createStructTypeCode(
            String name)
    {
        return new StructTypeCode(getScope(), name);
    }

    @Override
    public UnionTypeCode createUnionTypeCode(
            String scope,
            String name)
    {
        return new UnionTypeCode(scope, name);
    }

    @Override
    public UnionTypeCode createUnionTypeCode(
            String scope,
            String name,
            TypeCode discriminatorTypeCode)
    {
        return new UnionTypeCode(scope, name, discriminatorTypeCode);
    }

    @Override
    public void addTypeDeclaration(
            TypeDeclaration typedecl)
    {
        super.addTypeDeclaration(typedecl);

        if (typedecl.getTypeCode().getKind() == Kind.KIND_STRUCT && typedecl.isInScope())
        {
            Annotation topicann = typedecl.getAnnotations().get("Topic");

            if (topicann != null && topicann.getValue("value").equalsIgnoreCase("false"))
            {
                StructTypeCode structtypecode = (StructTypeCode)typedecl.getTypeCode();
                structtypecode.setIsTopic(false);
            }
            else
            {
                m_lastStructure = typedecl;
            }
        }
    }

    public boolean isClient()
    {
        return m_subscribercode;
    }

    public boolean isServer()
    {
        return m_publishercode;
    }

    // TODO For stringtemplate TopicsPlugin of our DDS types.
    public String getNewRandomName()
    {
        String name = "type_" + ++m_randomGenName;
        m_randomGenNames.push(name);
        return name;
    }

    public String getNewLoopVarName()
    {
        m_loopVarName = 'a';
        return Character.toString(m_loopVarName);
    }

    public String getNextLoopVarName()
    {
        return Character.toString(++m_loopVarName);
    }

    // TODO For stringtemplate TopicsPlugin of our DDS types.
    public String getLastRandomName()
    {
        return m_randomGenNames.pop();
    }

    public ArrayList<Entry<String, TypeCode>> getTypeCodesToDefine()
    {
        ArrayList<Entry<String, TypeCode>> typecodes = new ArrayList<Entry<String, TypeCode>>();

        for (TypeDeclaration type : m_types.values())
        {
            if (type.getTypeCode() instanceof MemberedTypeCode && !(type.getTypeCode() instanceof EnumTypeCode))
            {
                for (Member member : ((MemberedTypeCode)type.getTypeCode()).getMembers())
                {
                    if (member.getTypecode().getKind() == Kind.KIND_SEQUENCE)
                    {
                        getSequencesToDefine(typecodes, (SequenceTypeCode)member.getTypecode());
                    }
                    else if (member.getTypecode().getKind() == Kind.KIND_MAP)
                    {
                        MapTypeCode map = (MapTypeCode)member.getTypecode();
                        if (map.getKeyTypeCode().getKind() == Kind.KIND_SEQUENCE)
                        {
                            getSequencesToDefine(typecodes, (SequenceTypeCode)map.getKeyTypeCode());
                        }
                        if (map.getValueTypeCode().getKind() == Kind.KIND_SEQUENCE)
                        {
                            getSequencesToDefine(typecodes, (SequenceTypeCode)map.getValueTypeCode());
                        }
                    }
                }
            }
        }

        return typecodes;
    }

    private void getSequencesToDefine(
            ArrayList<Entry<String, TypeCode>> typecodes,
            SequenceTypeCode sequence)
    {
        // Search
        for (Entry<String, TypeCode> entry : typecodes)
        {
            if (entry.getKey().equals(sequence.getCppTypename()))
            {
                return;
            }
        }

        TypeCode content = sequence.getContentTypeCode();

        if (content.getKind() == Kind.KIND_SEQUENCE)
        {
            getSequencesToDefine(typecodes, (SequenceTypeCode)content);
        }
        else if (content.getKind() == Kind.KIND_MAP)
        {
            MapTypeCode map = (MapTypeCode)content;
            if (map.getKeyTypeCode().getKind() == Kind.KIND_SEQUENCE)
            {
                getSequencesToDefine(typecodes, (SequenceTypeCode)map.getKeyTypeCode());
            }
            if (map.getValueTypeCode().getKind() == Kind.KIND_SEQUENCE)
            {
                getSequencesToDefine(typecodes, (SequenceTypeCode)map.getValueTypeCode());
            }
        }

        typecodes.add(new SimpleEntry<String, TypeCode>(sequence.getCppTypename(), sequence));
    }

    public boolean isThereIsStructOrUnion()
    {
        for (TypeDeclaration type : m_types.values())
        {
            if (type.getTypeCode() instanceof StructTypeCode ||
                    type.getTypeCode() instanceof UnionTypeCode)
            {
                return true;
            }
        }

        return false;
    }

    /*** Functions inherited from FastCDR Context ***/

    @Override
    public boolean isPrintexception()
    {
        return false;
    }

    @Override
    public boolean isPrintoperation()
    {
        return false;
    }

    public String getProduct()
    {
        return "fastdds";
    }

    public String getNamespace()
    {
        return "fastcdr";
    }

    public boolean isCdr()
    {
        return true;
    }

    public boolean isFastcdr()
    {
        return activateFusion_;
    }

    public boolean isAnyCdr()
    {
        return true;
    }

    /*** End ***/

    public void setActivateFusion(
            boolean value)
    {
        activateFusion_ = value;
    }

    //// Java block ////
    public void setPackage(
            String pack)
    {
        if (pack != null && !pack.isEmpty())
        {
            m_package = pack + ".";
            m_onlypackage = pack;
            m_packageDir = m_package.replace('.', '/');
        }
    }

    public boolean isIsPackageEmpty()
    {
        return m_package.isEmpty();
    }

    public String getPackage()
    {
        return m_package;
    }

    public String getOnlyPackage()
    {
        return m_onlypackage;
    }

    public String getPackageDir()
    {
        return m_packageDir;
    }

    public String getPackageUnder()
    {
        return m_package.replace('.', '_');
    }

    //// End Java block ////

    private String m_typelimitation = null;

    //! Cache the first interface.
    private Interface m_firstinterface = null;
    //! Cache the first exception.
    private com.eprosima.idl.parser.tree.Exception m_firstexception = null;

    // TODO Counts generation of new names.
    private int m_randomGenName = 0;
    private Stack<String> m_randomGenNames = null;
    // TODO Keeps track of variable name for nested loops.
    private char m_loopVarName = 'a';

    // Stores if the user will generate the client source.
    private boolean m_subscribercode = true;
    // Stores if the user will generate the server source.
    private boolean m_publishercode = true;

    // TODO Remove
    private String m_appProduct = null;

    private TypeDeclaration m_lastStructure = null;

    private boolean m_type_object = false;

    private boolean m_typesc = false;

    private boolean m_type_ros2 = false;

    @Override
    public boolean isGenerateTypeObject()
    {
        return m_type_object;
    }

    @Override
    public boolean isGenerateTypesC()
    {
        return m_typesc;
    }

    @Override
    public boolean isGenerateTypesROS2()
    {
        return m_type_ros2;
    }

    public String getHeaderGuardName ()
    {
        if (m_lastStructure != null)
        {
            if (m_lastStructure.getHasScope())
            {
                return m_lastStructure.getScope().replaceAll("::", "_").toUpperCase() +
                       "_" + m_fileNameUpper.replaceAll("\\.", "_");
            }
        }
        return m_fileNameUpper;
    }

    public String getM_lastStructureTopicDataTypeName()
    {
        String name = new String("");

        if (m_lastStructure != null)
        {
            if (m_lastStructure.getParent() instanceof Interface)
            {
                name = name + ((Interface)m_lastStructure.getParent()).getScopedname() + "_" +
                        m_lastStructure.getName();
            }
            else
            {
                name = m_lastStructure.getScopedname();
            }
        }
        return name;
    }

    public String getM_lastStructureScopedName()
    {
        if (m_lastStructure != null)
        {
            return m_lastStructure.getScopedname();
        }
        return null;
    }

    public boolean isThereIsStructure()
    {
        if (m_lastStructure != null)
        {
            return true;
        }
        return false;
    }

    public TypeDeclaration getLastStructure()
    {
        return m_lastStructure;
    }

    public boolean existsLastStructure()
    {
        if (m_lastStructure != null)
        {
            return true;
        }
        return false;
    }

    private String m_fileNameUpper = null;

    public void setFilename(
            String filename)
    {
        super.setFilename(filename);
        m_fileNameUpper = filename.toUpperCase();
    }

    public String getFileNameUpper()
    {
        return m_fileNameUpper;
    }

    public String getJniFilename()
    {
        return getFilename().replace("_", "_1");
    }

    //// Java block ////
    // Java package name.
    private String m_package = "";
    private String m_onlypackage = "";
    // Java package dir.
    private String m_packageDir = "";
    private boolean activateFusion_ = false;
    //// End Java block

}
