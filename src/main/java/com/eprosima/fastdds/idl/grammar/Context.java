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
import com.eprosima.idl.generator.manager.TemplateGroup;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.parser.tree.Annotation;
import com.eprosima.idl.parser.tree.AnnotationDeclaration;
import com.eprosima.idl.parser.tree.AnnotationMember;
import com.eprosima.idl.parser.tree.Interface;
import com.eprosima.idl.parser.tree.TypeDeclaration;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.TypeCode;
import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.typecode.MemberedTypeCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.Token;


public class Context extends com.eprosima.idl.context.Context implements com.eprosima.fastcdr.idl.context.Context
{
    // TODO Remove middleware parameter. It is temporal while cdr and rest don't have async functions.
    public Context(
            TemplateManager tmanager,
            String file,
            ArrayList<String> includePaths,
            boolean subscribercode,
            boolean publishercode,
            String appProduct,
            boolean generate_typesc,
            boolean generate_type_ros2,
            boolean is_generating_api,
            boolean generate_typeobjectsupport
            )
    {
        super(tmanager, file, includePaths, generate_typesc);
        m_fileNameUpper = getFilename().toUpperCase();
        m_subscribercode = subscribercode;
        m_publishercode = publishercode;
        m_randomGenNames = new Stack<String>();

        // TODO Remove
        m_appProduct = appProduct;
        //m_protocol = protocol;
        //m_ddstypes = ddstypes;

        m_type_ros2 = generate_type_ros2;
        is_generating_api_ = is_generating_api;
        generate_typeobject_support_ = generate_typeobjectsupport;

        // Create default @Key annotation.
        AnnotationDeclaration keyann = this.createAnnotationDeclaration(Annotation.eprosima_key_str, null);
        keyann.addMember(new AnnotationMember(Annotation.value_str, new PrimitiveTypeCode(Kind.KIND_BOOLEAN), Annotation.true_str));

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
    public TemplateGroup addModule(
            com.eprosima.idl.parser.tree.Module module)
    {
        if (!is_generating_api_)
        {
            return super.addModule(module);
        }
        else
        {
            List<String> new_modules = modules_conversion.get(module.getName());

            if (null ==  new_modules)
            {
                return super.addModule(module);
            }
            else
            {
                com.eprosima.idl.parser.tree.Module last_module = null;
                TemplateGroup module_template = null;
                ArrayList<com.eprosima.idl.parser.tree.Module> module_list = new ArrayList<com.eprosima.idl.parser.tree.Module>();
                for(String new_module : new_modules)
                {
                    if (null == last_module)
                    {
                        last_module = createModule(module.getScopeFile(), module.isInScope(),
                                module.getScope(), new_module, module.getToken());
                    }
                    else
                    {
                        last_module = createModule(last_module.getScopeFile(), last_module.isInScope(), last_module.getScope(),
                                new_module, last_module.getToken());
                    }

                    super.addModule(last_module);
                    module_list.add(last_module);
                }

                if(isInScopedFile() || isScopeLimitToAll()) {
                    if(tmanager_ != null) {
                        module_template = tmanager_.createTemplateGroup("module_conversion");
                        module_template.setAttribute("ctx", this);
                        // Set the module object to the TemplateGroup of the module.
                        module_template.setAttribute("modules", module_list);
                    }
                }

                return module_template;
            }
        }
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
        if (isInScopedFile())
        {
            there_is_at_least_one_array = true;
        }
        return new ArrayTypeCode();
    }

    @Override
    public BitsetTypeCode createBitsetTypeCode(
            String scope,
            String name)
    {
        if (isInScopedFile())
        {
            there_is_at_least_one_bitset = true;
        }
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
        if (isInScopedFile())
        {
            there_is_at_least_one_map = true;
        }
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
        if (isInScopedFile())
        {
            there_is_at_least_one_sequence = true;
        }
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
        if (isInScopedFile())
        {
            there_is_at_least_one_string = true;
        }
        return new StringTypeCode(kind, maxsize, evaluate_literal(maxsize));
    }

    @Override
    public StructTypeCode createStructTypeCode(
            String name)
    {
        if (isInScopedFile())
        {
            there_is_at_least_one_struct = true;
        }
        return new StructTypeCode(getScope(), name);
    }

    @Override
    public UnionTypeCode createUnionTypeCode(
            String scope,
            String name)
    {
        if (isInScopedFile())
        {
            there_is_at_least_one_union = true;
        }
        return new UnionTypeCode(scope, name);
    }

    @Override
    public UnionTypeCode createUnionTypeCode(
            String scope,
            String name,
            TypeCode discriminatorTypeCode)
    {
        if (isInScopedFile())
        {
            there_is_at_least_one_union = true;
        }
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

        for (TypeDeclaration type : getTypes())
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

    public boolean isThereIsArray()
    {
        return there_is_at_least_one_array;
    }

    public boolean isThereIsBitset()
    {
        return there_is_at_least_one_bitset;
    }

    public boolean isThereIsExternalAnnotation()
    {
        return there_is_at_least_one_external_annotation;
    }

    public boolean isThereIsMap()
    {
        return there_is_at_least_one_map;
    }

    public boolean isThereIsOptionalAnnotation()
    {
        return there_is_at_least_one_optional_annotation;
    }

    public boolean isThereIsSequence()
    {
        return there_is_at_least_one_sequence;
    }

    public boolean isThereIsString()
    {
        return there_is_at_least_one_string;
    }

    public boolean isThereIsStructure()
    {
        return there_is_at_least_one_struct;
    }

    public boolean isThereIsUnion()
    {
        return there_is_at_least_one_union;
    }

    public boolean isThereIsStructOrUnion()
    {
        return there_is_at_least_one_struct || there_is_at_least_one_union;
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

    private boolean m_type_ros2 = false;

    private boolean generate_typeobject_support_ = true;

    @Override
    public boolean isGenerateTypesROS2()
    {
        return m_type_ros2;
    }

    @Override
    public boolean isGenerateTypeObjectSupport()
    {
        return generate_typeobject_support_;
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

    @Override
    public TypeCode getTypeCode(
            String name)
    {
        if (!is_generating_api_)
        {
            return super.getTypeCode(name);
        }
        else
        {
            String current_name = name;

            for (Map.Entry<String, List<String>> entry : modules_conversion.entrySet())
            {
                // Additional replacement logic to avoid double replacements
                if (!current_name.contains(String.join("::", entry.getValue())))
                {
                    current_name = current_name.replace(entry.getKey() + "::", String.join("::", entry.getValue()) + "::");
                }
            }

            return super.getTypeCode(current_name);
        }
    }

    @Override
    public AnnotationDeclaration getAnnotationDeclaration(
            String name)
    {
        if (isInScopedFile())
        {
            if (name.equals(Annotation.optional_str))
            {
                there_is_at_least_one_optional_annotation = true;
            }
            else if (name.equals(Annotation.external_str))
            {
                there_is_at_least_one_external_annotation = true;
            }
        }

        return super.getAnnotationDeclaration(name);
    }

    //// Java block ////
    // Java package name.
    private String m_package = "";
    private String m_onlypackage = "";
    // Java package dir.
    private String m_packageDir = "";
    private boolean activateFusion_ = false;
    //// End Java block

    private boolean cdr_v1_templates = false;

    private boolean is_generating_api_ = false;

    private Map<String, List<String>> modules_conversion = Stream.of(
        new AbstractMap.SimpleEntry<>("dds", Arrays.asList("eprosima", "fastdds", "dds")),
        new AbstractMap.SimpleEntry<>("DDS", Arrays.asList("eprosima", "fastdds", "dds")),
        new AbstractMap.SimpleEntry<>("XTypes", Arrays.asList("xtypes")))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private boolean there_is_at_least_one_array = false;

    private boolean there_is_at_least_one_bitset = false;

    private boolean there_is_at_least_one_external_annotation = false;

    private boolean there_is_at_least_one_map = false;

    private boolean there_is_at_least_one_optional_annotation = false;

    private boolean there_is_at_least_one_sequence = false;

    private boolean there_is_at_least_one_string = false;

    private boolean there_is_at_least_one_struct = false;

    private boolean there_is_at_least_one_union = false;
}
