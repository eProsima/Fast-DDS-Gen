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

package com.eprosima.fastdds;

import com.eprosima.fastcdr.idl.generator.TypesGenerator;
import com.eprosima.fastdds.exceptions.BadArgumentException;
import com.eprosima.fastdds.idl.grammar.Context;
import com.eprosima.fastdds.solution.Project;
import com.eprosima.fastdds.solution.Solution;
import com.eprosima.fastdds.util.Utils;
import com.eprosima.fastdds.util.VSConfiguration;
import com.eprosima.idl.generator.manager.TemplateGroup;
import com.eprosima.idl.generator.manager.TemplateManager;
import com.eprosima.idl.generator.manager.TemplateST;
import com.eprosima.idl.parser.grammar.IDLLexer;
import com.eprosima.idl.parser.grammar.IDLParser;
import com.eprosima.idl.parser.tree.Annotation;
import com.eprosima.idl.parser.tree.Specification;
import com.eprosima.idl.parser.typecode.Kind;
import com.eprosima.idl.parser.typecode.ContainerTypeCode;
import com.eprosima.idl.parser.typecode.PrimitiveTypeCode;
import com.eprosima.idl.parser.typecode.TypeCode;
import com.eprosima.idl.util.Util;
import com.eprosima.log.ColorMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.IOError;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import org.stringtemplate.v4.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;




// TODO: Implement Solution & Project in com.eprosima.fastdds.solution

public class fastddsgen
{

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Attributes
     */

    private static ArrayList<String> m_platforms = null;

    private Vector<String> m_idlFiles;
    protected static String m_appEnv = "FASTDDSHOME";
    private String m_exampleOption = null;
    private boolean m_ppDisable = false; //TODO
    private boolean m_replace = false;
    private String m_ppPath = null;
    private final String m_defaultOutputDir = "." + File.separator;
    private String m_outputDir = m_defaultOutputDir;
    private String m_tempDir = null;
    protected static String m_appName = "fastddsgen";

    private boolean m_publishercode = true;
    private boolean m_subscribercode = true;
    private boolean m_atLeastOneStructure = false;
    protected static String m_localAppProduct = "fastdds";
    private ArrayList<String> m_includePaths = new ArrayList<String>();

    // Mapping where the key holds the path to the template file and the value the wanted output file name
    private Map<String, String> m_customStgOutput = new HashMap<String, String>();

    private static VSConfiguration m_vsconfigurations[] = {
        new VSConfiguration("Debug DLL", "Win32", true, true),
        new VSConfiguration("Release DLL", "Win32", false, true),
        new VSConfiguration("Debug", "Win32", true, false),
        new VSConfiguration("Release", "Win32", false, false)
    };

    private String m_os = null;
    private boolean fusion_ = false;

    //! Default package used in Java files.
    private String m_package = "";

    // Generates type naming compatible with ROS 2
    private boolean m_type_ros2 = false;

    // Generate string and sequence types compatible with C?
    private boolean m_typesc = false;

    // Generate python binding files
    private boolean m_python = false;

    private boolean m_case_sensitive = false;

    // Testing
    private boolean m_test = false;

    // Ignore input relative file paths
    // generate files under the -d directory
    // ignoring the relative dir of the input files
    private boolean m_flat_output_dir = false;

    // Generating internal API
    private boolean gen_api_ = false;

    // Use to know the programming language
    public enum LANGUAGE
    {
        CPP,
        JAVA
    };

    private LANGUAGE m_languageOption = LANGUAGE.CPP; // Default language -> CPP

    // Specifies whether the type support files should be generated.
    private boolean generate_typesupport_ = true;

    // Specifies whether the dependent IDL files should be processed.
    private boolean generate_dependencies_ = true;

    // Specifies whether the TypeObject Support files should be generated.
    private boolean generate_typeobjectsupport_ = true;

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Constructor
     */

    public fastddsgen(
            String [] args) throws BadArgumentException
    {

        int count = 0;
        String arg;

        // Detect OS
        m_os = System.getProperty("os.name");

        m_idlFiles = new Vector<String>();

        // Check arguments
        while (count < args.length)
        {

            arg = args[count++];

            if (!arg.startsWith("-"))
            {
                m_idlFiles.add(Paths.get(arg).normalize().toString());
            }
            else if (arg.equals(case_sensitive_arg))
            {
                m_case_sensitive = true;
            }
            else if (arg.equals(output_path_arg))
            {
                if (count < args.length)
                {
                    m_outputDir = Utils.addFileSeparator(args[count++]);
                }
                else
                {
                    throw new BadArgumentException("No URL specified after " + output_path_arg + " argument");
                }
            }
            else if (arg.equals(default_container_prealloc_size))
            {
                if (count < args.length)
                {
                    ContainerTypeCode.default_unbounded_max_size = args[count++];
                }
                else
                {
                    throw new BadArgumentException("No value specified after " + default_container_prealloc_size + " argument");
                }
            }
            else if (arg.equals(default_extensibility_short_arg) || arg.equals(default_extensibility_arg))
            {
                if (count < args.length)
                {
                    String extensibility = args[count++];
                    if (extensibility.equals(Annotation.final_str))
                    {
                        TypeCode.default_extensibility = TypeCode.ExtensibilityKind.FINAL;
                    }
                    else if (extensibility.equals(Annotation.appendable_str))
                    {
                        TypeCode.default_extensibility = TypeCode.ExtensibilityKind.APPENDABLE;
                    }
                    else if (extensibility.equals(Annotation.mutable_str))
                    {
                        TypeCode.default_extensibility = TypeCode.ExtensibilityKind.MUTABLE;
                    }
                    else
                    {
                        throw new BadArgumentException("Extensibility value " + extensibility + " is not valid");
                    }
                }
                else
                {
                    throw new BadArgumentException("No extensibility value after " + default_extensibility_arg + " argument");
                }
            }
            else if (arg.equals(specific_platform_arg))
            {
                if (count < args.length)
                {
                    m_exampleOption = args[count++];
                    if (!m_platforms.contains(m_exampleOption))
                    {
                        throw new BadArgumentException("Unknown example arch " + m_exampleOption);
                    }
                }
                else
                {
                    throw new BadArgumentException("No architecture speficied after " + specific_platform_arg + " argument");
                }
            }
            else if (arg.equals(extra_template_arg))
            {
                if (count + 1 < args.length)
                {
                    m_customStgOutput.put(args[count++], args[count++]);
                }
                else
                {
                    throw new BadArgumentException("Missing arguments for " + extra_template_arg);
                }
            }
            else if (arg.equals(flat_output_directory_arg))
            {
                m_flat_output_dir = true;
            }
            else if (arg.equals(generate_api_arg))
            {
                gen_api_ = true;
            }
            else if (arg.equals(help_arg))
            {
                printHelp();
                System.exit(0);
            }
            else if (arg.equals("-help+"))
            {
                printEnhacedHelp();
                System.exit(0);
            }
            else if (arg.equals(include_path_arg))
            {
                if (count < args.length)
                {
                    String pathStr = args[count++];
                    if (!isIncludePathDuplicated(pathStr)) {
                        m_includePaths.add(include_path_arg.concat(pathStr));
                    }
                }
                else
                {
                    throw new BadArgumentException("No include directory specified after " + include_path_arg + " argument");
                }
            }
            else if (arg.equals(language_arg))
            {
                if (count < args.length)
                {
                    String languageOption = args[count++];

                    if (languageOption.equalsIgnoreCase("c++"))
                    {
                        m_languageOption = LANGUAGE.CPP;
                    }
                    else if (languageOption.equalsIgnoreCase("java"))
                    {
                        m_languageOption = LANGUAGE.JAVA;
                    }
                    else
                    {
                        throw new BadArgumentException("Unknown language " +  languageOption);
                    }
                }
                else
                {
                    throw new BadArgumentException("No language specified after " + language_arg + " argument");
                }
            }
            else if (arg.equals(no_typesupport_arg))
            {
                generate_typesupport_ = false;
                generate_typeobjectsupport_ = false;
            }
            else if (arg.equals(no_dependencies_arg))
            {
                generate_dependencies_ = false;
            }
            else if (arg.equals(no_typeobjectsupport_arg))
            {
                generate_typeobjectsupport_ = false;
            }
            else if (arg.equals(package_arg))
            {
                if (count < args.length)
                {
                    m_package = args[count++];
                }
                else
                {
                    throw new BadArgumentException("No package after " + package_arg + " argument");
                }
            }
            else if (arg.equals(disable_preprocessor_arg))
            {
                m_ppDisable = true;
            }
            else if (arg.equals(preprocessor_path_arg))
            {
                if (count < args.length)
                {
                    m_ppPath = args[count++];
                }
                else
                {
                    throw new BadArgumentException("No URL specified after " + preprocessor_path_arg + " argument");
                }
            }
            else if (arg.equals(python_bindings_arg))
            {
                m_python = true;
            }
            else if (arg.equals(replace_arg))
            {
                m_replace = true;
            }
            else if (arg.equals(temp_dir_arg))
            {
                if (count < args.length)
                {
                    m_tempDir = Utils.addFileSeparator(args[count++]);
                }
                else
                {
                    throw new BadArgumentException("No temporary directory specified after "+ temp_dir_arg +" argument");
                }
            }
            else if (arg.equals(execute_test_arg))
            {
                m_test = true;
            }
            else if (arg.equals(ros2_names_arg))
            {
                m_type_ros2 = true;
                TypeCode.default_extensibility = TypeCode.ExtensibilityKind.FINAL;
            }
            else if (arg.equals(cnames_arg))
            {
                m_typesc = true;
            }
            else if (arg.equals(version_arg))
            {
                showVersion();
                System.exit(0);
            }
            else if (arg.equals(fusion_arg))
            {
                fusion_ = true;
            }
            else   // TODO: More options: -rpm, -debug
            {
                throw new BadArgumentException("Unknown argument " + arg);
            }

        }

        if (null != m_exampleOption && m_python)
        {
            throw new BadArgumentException(specific_platform_arg + " and " + python_bindings_arg + " currently are incompatible");
        }

        if (m_idlFiles.isEmpty())
        {
            throw new BadArgumentException("No input files given");
        }

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main methods
     */

    public boolean execute()
    {


        if (!m_outputDir.equals(m_defaultOutputDir))
        {
            File dir = new File(m_outputDir);

            if (!dir.exists())
            {
                System.out.println(ColorMessage.error() + "The specified output directory does not exist");
                return false;
            }
        }

        boolean returnedValue = globalInit();

        if (returnedValue)
        {
            Solution solution = new Solution(m_languageOption, m_exampleOption,
                            getVersion(), m_publishercode, m_subscribercode);

            // In local for all products
            if (m_os.contains("Windows"))
            {
                solution.addInclude("$(" + m_appEnv + ")/include");
                solution.addLibraryPath("$(" + m_appEnv + ")/lib");
                if (m_exampleOption != null)
                {
                    solution.addLibraryPath("$(" + m_appEnv + ")/lib/" + m_exampleOption);
                    solution.addLibraryPath("$(" + m_appEnv + ")/lib/" + m_exampleOption + "/VC/static");
                }
            }

            // If Java, include jni headers
            if (m_languageOption == LANGUAGE.JAVA)
            {
                solution.addInclude("$(JAVA_HOME)/include");

                if (m_exampleOption != null && m_exampleOption.contains("Linux"))
                {
                    solution.addInclude("$(JAVA_HOME)/include/linux");
                }
            }

            if ((m_exampleOption != null || m_test) && !m_exampleOption.contains("Win"))
            {
                solution.addLibrary("fastcdr");
            }

            // Add product library
            solution.addLibrary("fastdds");

            for (int count = 0; returnedValue && (count < m_idlFiles.size()); ++count)
            {
                Project project = process(m_idlFiles.get(count), null, true);

                if (project != null)
                {
                    System.out.println("Adding project: " + project.getFile());
                    if (!solution.existsProject(project.getFile()))
                    {
                        solution.addProject(project);
                    }
                    if(generate_dependencies_)
                    {
                        for (String include : project.getIDLIncludeFiles())
                        {
                            Project inner = process(include, Util.getIDLFileDirectoryOnly(m_idlFiles.get(count)), false);
                            if (inner != null && !solution.existsProject(inner.getFile()))
                            {
                                System.out.println("Adding project: " + inner.getFile());
                                solution.addProject(inner);
                            }
                        }
                    }
                }
                else
                {
                    returnedValue = false;
                }
            }

            if (returnedValue && m_python)
            {
                returnedValue = genSwigCMake(solution);
            }


            // Generate solution
            if (returnedValue && (m_exampleOption != null) || m_test)
            {
                if ((returnedValue = genSolution(solution)) == false)
                {
                    System.out.println(ColorMessage.error() + "While the solution was being generated");
                }
            }

        }

        return returnedValue;

    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Auxiliary methods
     */

    public static boolean loadPlatforms()
    {

        boolean returnedValue = false;

        fastddsgen.m_platforms = new ArrayList<String>();

        fastddsgen.m_platforms.add("i86Win32VS2019");
        fastddsgen.m_platforms.add("x64Win64VS2019");
        fastddsgen.m_platforms.add("CMake");

        returnedValue = true;

        return returnedValue;
    }

    private String getVersion()
    {
        try
        {
            InputStream input = this.getClass().getClassLoader().getResourceAsStream("version");
            byte[] b = new byte[input.available()];
            input.read(b);
            String text = new String(b);
            int beginindex = text.indexOf("=");
            return text.substring(beginindex + 1);
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error() + "Getting version. " + ex.getMessage());
        }

        return "";
    }

    private void showVersion()
    {
        String version = getVersion();
        System.out.println(m_appName + " version " + version);
    }

    private boolean isIncludePathDuplicated(String pathToCheck) {
        try {
            Path path = Paths.get(pathToCheck);
            String absPath = path.toAbsolutePath().toString();
            boolean isDuplicateFound = false;
            for (String includePath : m_includePaths) {
                // include paths are prefixed with "-I"
                if (includePath.length() <= 2) {
                    continue;
                }
                String absIncludePath = Paths.get(includePath.substring(2)).toAbsolutePath().toString();
                if (absPath.toLowerCase().equals(absIncludePath.toLowerCase())) {
                    isDuplicateFound = true;
                    break;
                }
            }
            if (isDuplicateFound) {
                return true;
            }
        } catch (InvalidPathException | IOError | SecurityException ex) {
            // path operations failed, just returning false
        }
        return false;
    }
        
    /*
     * ----------------------------------------------------------------------------------------
     * Arguments
     */
    private static final String case_sensitive_arg = "-cs";
    private static final String output_path_arg = "-d";
    private static final String default_container_prealloc_size = "-default-container-prealloc-size";
    private static final String default_extensibility_arg = "-default_extensibility";
    private static final String default_extensibility_short_arg = "-de";
    private static final String specific_platform_arg = "-example";
    private static final String extra_template_arg = "-extrastg";
    private static final String flat_output_directory_arg = "-flat-output-dir";
    private static final String fusion_arg = "-fusion";
    private static final String help_arg = "-help";
    private static final String include_path_arg = "-I";
    private static final String language_arg = "-language";
    private static final String no_typesupport_arg = "-no-typesupport";
    private static final String no_typeobjectsupport_arg = "-no-typeobjectsupport";
    private static final String no_dependencies_arg = "-no-dependencies";
    private static final String package_arg = "-package";
    private static final String disable_preprocessor_arg = "-ppDisable";
    private static final String preprocessor_path_arg = "-ppPath";
    private static final String python_bindings_arg = "-python";
    private static final String replace_arg = "-replace";
    private static final String temp_dir_arg = "-t";
    private static final String ros2_names_arg = "-typeros2";
    private static final String cnames_arg = "-typesc";
    private static final String version_arg = "-version";

    /*
     * ----------------------------------------------------------------------------------------
     * Developer Arguments
     */
    private static final String generate_api_arg = "-genapi";
    private static final String execute_test_arg = "-test";

    public static void printHelp()
    {
        System.out.println(m_appName + " usage:");
        System.out.println("\t" + m_appName + " [options] <file> [<file> ...]");
        System.out.println("\tThe available options are:");
        System.out.println("\t\t" + case_sensitive_arg + ": IDL grammar apply case sensitive matching.");
        System.out.println("\t\t" + output_path_arg + " <path>: sets an output directory for generated files.");
        System.out.print("\t\t" + default_container_prealloc_size + ": sets the default preallocated size for ");
        System.out.println("containers (sequence and maps). Default value: 0");
        System.out.print("\t\t" + default_extensibility_arg + " | " + default_extensibility_short_arg + " <ext>: ");
        System.out.println("sets the default extensibility for types without the @extensibility annotation.");
        System.out.println("\t\t Values:");
        System.out.println("\t\t\t* " + Annotation.final_str);
        System.out.println("\t\t\t* " + Annotation.appendable_str + " (default)");
        System.out.println("\t\t\t* " + Annotation.mutable_str);
        System.out.print("\t\t" + specific_platform_arg + " <platform>: Generates a solution for a specific ");
        System.out.println("platform (example: x64Win64VS2019)");
        System.out.println("\t\t\tSupported platforms:");
        for (int count = 0; count < m_platforms.size(); ++count)
        {
            System.out.println("\t\t\t * " + m_platforms.get(count));
        }
        System.out.print("\t\t" + extra_template_arg + " <template file> <output file name>: specifies a custom ");
        System.out.println("template, template location must be in classpath.");
        System.out.print("\t\t" + flat_output_directory_arg + ": ignore input files relative paths and place all ");
        System.out.println("generated files in the specified output directory.");
        System.out.println("\t\t" + fusion_arg + ": activates fusion.");
        System.out.println("\t\t" + help_arg + ": shows this help");
        System.out.println("\t\t" + include_path_arg + " <path>: add directory to preprocessor include paths.");
        System.out.println("\t\t" + language_arg + " <lang>: chooses between <c++> or <java> languages.");
        System.out.println("\t\t" + no_typesupport_arg + ": avoid generating the type support files.");
        System.out.println("\t\t" + no_typeobjectsupport_arg + ": avoid generating the TypeObject support files.");
        System.out.println("\t\t\tEnabled automatically if " + no_typesupport_arg + " argument is used.");
        System.out.println("\t\t" + no_dependencies_arg + ": avoid processing the dependent IDL files.");
        System.out.println("\t\t" + package_arg + ": default package used in Java files.");
        System.out.println("\t\t" + disable_preprocessor_arg + ": disables the preprocessor.");
        System.out.println("\t\t" + preprocessor_path_arg + ": specifies the preprocessor path.");
        System.out.println("\t\t" + python_bindings_arg + ": generates python bindings for the generated types.");
        System.out.println("\t\t" + replace_arg + ": replaces existing generated files.");
        System.out.println("\t\t" + temp_dir_arg + " <temp dir>: sets a specific directory as a temporary directory.");
        System.out.println("\t\t" + ros2_names_arg + ": generates type naming compatible with ROS2.");
        System.out.println("\t\t" + cnames_arg + ": generates string and sequence types compatible with C.");
        System.out.println("\t\t" + version_arg + ": shows the current version of eProsima Fast DDS gen.");
        System.out.println("\tThe supported input files are:");
        System.out.println("\t* IDL files.");

    }

    public static void printEnhacedHelp()
    {
        printHelp();
        System.out.println("\tThe extra developer options are:");
        System.out.println("\t\t" + generate_api_arg + ": apply rules to generate internal API.");
        System.out.println("\t\t" + execute_test_arg + ": executes FastDDSGen tests.");
    }

    public boolean globalInit()
    {

        // Set the temporary folder
        if (m_tempDir == null)
        {
            if (m_os.contains("Windows"))
            {
                String tempPath = System.getenv("TEMP");

                if (tempPath == null)
                {
                    tempPath = System.getenv("TMP");
                }

                m_tempDir = tempPath;
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                m_tempDir = "/tmp/";
            }
        }

        if (m_tempDir.charAt(m_tempDir.length() - 1) != File.separatorChar)
        {
            m_tempDir += File.separator;
        }

        return true;
    }

    private Project process(
            String idlFilename,
            String dependant_idl_dir,
            boolean processCustomTemplates)
    {
        Project project = null;
        System.out.println("Processing the file " + idlFilename + "...");

        try
        {
            project = parseIDL(idlFilename, dependant_idl_dir, processCustomTemplates);
        }
        catch (Exception ioe)
        {
            System.out.println(ColorMessage.error() + "Cannot generate the files");
            if (!ioe.getMessage().equals(""))
            {
                System.out.println(ioe.getMessage());
            }
            ioe.printStackTrace(System.err);
        }

        return project;

    }

    private Project parseIDL(
            String idlFilename,
            String dependant_idl_dir,
            boolean processCustomTemplates)
    {
        boolean returnedValue = false;
        String idlParseFileName = idlFilename;
        Project project = null;

        if (!m_ppDisable)
        {
            idlParseFileName = callPreprocessor(idlFilename);
        }

        if (idlParseFileName != null)
        {
            // Create template manager
            TemplateManager tmanager = new TemplateManager();

            Context ctx = new Context(tmanager, idlFilename, m_includePaths, m_subscribercode, m_publishercode,
                            m_localAppProduct, m_typesc, m_type_ros2, gen_api_, generate_typeobjectsupport_);

            String relative_dir = ctx.getRelativeDir(dependant_idl_dir);
            String output_dir;
            if (!m_flat_output_dir)
            {
                output_dir = m_outputDir + relative_dir;
            }
            else
            {
                output_dir = m_outputDir;
            }

            // Check the output directory exists or create it.
            File dir = new File(output_dir);

            if (!dir.exists())
            {
                if (!dir.mkdirs())
                {
                    System.out.println(ColorMessage.error() + "Directory " + output_dir + " cannot be created");
                    return null;
                }
            }


            if (m_case_sensitive)
            {
                ctx.ignore_case(false);
            }

            if (fusion_)
            {
                ctx.setActivateFusion(true);
            }

            // Load common types template
            tmanager.addGroup("com/eprosima/fastcdr/idl/templates/TypesHeader.stg").enable_custom_property(
                    Context.using_explicitly_modules_custom_property);

            // Load Types common templates
            if (generate_typesupport_)
            {
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/TypesCdrAuxHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/TypesCdrAuxHeaderImpl.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPubSubTypeHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPubSubTypeSource.stg");

                if (generate_typeobjectsupport_)
                {
                    tmanager.addGroup("com/eprosima/fastdds/idl/templates/XTypesTypeObjectHeader.stg");
                    tmanager.addGroup("com/eprosima/fastdds/idl/templates/XTypesTypeObjectSource.stg");
                }
            }

            if (m_exampleOption != null)
            {
                // Load Application templates
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSApplicationHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSApplicationSource.stg");

                // Load Publisher templates
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPublisherHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPublisherSource.stg");

                // Load Subscriber templates
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSSubscriberHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSSubscriberSource.stg");

                // Load main template
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPubSubMain.stg");
            }

            if (m_test)
            {
                // Load test template
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/SerializationTestSource.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/SerializationHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/SerializationSource.stg");

                if (generate_typeobjectsupport_)
                {
                    // Load TypeObjectSupport test template
                    tmanager.addGroup("com/eprosima/fastdds/idl/templates/TypeObjectTestingTestSource.stg");
                }
            }

            // Add JNI sources.
            if (m_languageOption == LANGUAGE.JAVA)
            {
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/JNIHeader.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/JNISource.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/JavaSource.stg");

                // Set package in context.
                ctx.setPackage(m_package);
            }

            if (m_python)
            {
                tmanager.addGroup("com/eprosima/fastcdr/idl/templates/TypesSwigInterface.stg");
                tmanager.addGroup("com/eprosima/fastdds/idl/templates/DDSPubSubTypeSwigInterface.stg");
            }

            // Load custom templates into manager
            if (processCustomTemplates)
            {
                for (Map.Entry<String, String> entry : m_customStgOutput.entrySet())
                {
                    System.out.println("Loading custom template " + entry.getKey() + "...");
                    Path path = Paths.get(entry.getKey());
                    String templateName = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
                    try {
                        String content = new String(Files.readAllBytes(path));
                        tmanager.addGroupFromString(templateName, content);
                    } catch(IOException e){
                        System.out.println(ColorMessage.error(
                                "IOException") + "Cannot read content from " + path.toString());
                    }
                }
            }

            // Create main template
            TemplateGroup maintemplates = tmanager.createTemplateGroup("main");
            maintemplates.setAttribute("ctx", ctx);

            try
            {
                CharStream input = CharStreams.fromFileName(idlParseFileName);
                IDLLexer lexer = new IDLLexer(input);
                lexer.setContext(ctx);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                IDLParser parser = new IDLParser(tokens);
                // Pass the filename without the extension
                Specification specification = parser.specification(ctx, tmanager, maintemplates).spec;
                returnedValue = specification != null && !tmanager.get_st_error();;

            }
            catch (FileNotFoundException ex)
            {
                System.out.println(ColorMessage.error(
                            "FileNotFounException") + "The File " + idlParseFileName + " was not found.");
                returnedValue = false;
            }
            catch (IOException ex)
            {
                System.out.println(ColorMessage.error("Exception") + ex.getMessage());
                returnedValue = false;
            }

            if (returnedValue)
            {
                // Create information of project for solution
                project = new Project(ctx, idlFilename, ctx.getDependencies());

                // Create all custom files for template
                if (processCustomTemplates)
                {
                    for (Map.Entry<String, String> entry : m_customStgOutput.entrySet())
                    {
                        Path path = Paths.get(entry.getKey());
                        String templateName = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
                        System.out.println("Generating from custom " + templateName + " to " + entry.getValue());

                        if (returnedValue = Utils.writeFile(output_dir + entry.getValue(), maintemplates.getTemplate(templateName), m_replace))
                        {
                            // Try to determine if the file is a header file.
                            if (entry.getValue().contains(".hpp") || entry.getValue().contains(".h"))
                            {
                                project.addCommonIncludeFile(relative_dir + entry.getValue());
                            }
                            else
                            {
                                project.addCommonSrcFile(relative_dir + ctx.getFilename() + entry.getValue());
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                }

                System.out.println("Generating Type definition files...");
                if ((returnedValue) &&
                        (returnedValue = Utils.writeFile(output_dir + ctx.getFilename() + ".hpp",
                        maintemplates.getTemplate("com/eprosima/fastcdr/idl/templates/TypesHeader.stg"),
                        m_replace)))
                {
                    project.addCommonIncludeFile(relative_dir + ctx.getFilename() + ".hpp");

                    if (m_python)
                    {
                        System.out.println("Generating Swig interface files...");
                        if (returnedValue =
                                Utils.writeFile(output_dir + ctx.getFilename() + ".i",
                                    maintemplates.getTemplate("com/eprosima/fastcdr/idl/templates/TypesSwigInterface.stg"), m_replace))
                        {
                        }
                    }
                }

                if (m_test)
                {
                    System.out.println("Generating Serialization Test file...");
                    String fileName = output_dir + ctx.getFilename() + "SerializationTest.cpp";
                    returnedValue =
                            Utils.writeFile(fileName, maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/SerializationTestSource.stg"), m_replace);
                    project.addCommonTestingFile(relative_dir + ctx.getFilename() + "SerializationTest.cpp");

                    System.out.println("Generating Serialization Source file...");
                    String fileNameS = output_dir + ctx.getFilename() + "Serialization.cpp";
                    returnedValue =
                            Utils.writeFile(fileNameS, maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/SerializationSource.stg"), m_replace);
                    project.addCommonTestingFile(relative_dir + ctx.getFilename() + "Serialization.cpp");

                    System.out.println("Generating Serialization Header file...");
                    String fileNameH = output_dir + ctx.getFilename() + "Serialization.hpp";
                    returnedValue =
                            Utils.writeFile(fileNameH, maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/SerializationHeader.stg"), m_replace);

                    for (String element : project.getFullDependencies())
                    {
                        String trimmedElement = element.substring(0, element.length() - 4);// Remove .idl
                        project.addCommonTestingFile(trimmedElement + "Serialization.cpp");
                    }

                    if (generate_typeobjectsupport_)
                    {
                        System.out.println("Generating TypeObjects Test file...");
                        String fileNameTO = output_dir + ctx.getFilename() + "TypeObjectTestingTest.cpp";
                        returnedValue = Utils.writeFile(fileNameTO, maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/TypeObjectTestingTestSource.stg"), m_replace);
                        project.addTypeObjectTestingFile(relative_dir + ctx.getFilename() + "TypeObjectTestingTest.cpp");
                    }
                }

                System.out.println("Generating Type Support files...");
                if (generate_typesupport_)
                {
                    if (generate_typeobjectsupport_)
                    {
                        System.out.println("Generating TypeObjectSupport files...");
                        if (returnedValue &= Utils.writeFile(output_dir + ctx.getFilename() + "TypeObjectSupport.hpp",
                                maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/XTypesTypeObjectHeader.stg"), m_replace))
                        {
                            if (returnedValue &= Utils.writeFile(output_dir + ctx.getFilename() + "TypeObjectSupport.cxx",
                                    maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/XTypesTypeObjectSource.stg"), m_replace))
                            {
                                project.addCommonIncludeFile(relative_dir + ctx.getFilename() + "TypeObjectSupport.hpp");
                                project.addCommonSrcFile(relative_dir + ctx.getFilename() + "TypeObjectSupport.cxx");
                            }
                        }
                    }

                    if (ctx.isThereIsStructOrUnion())
                    {
                        if (returnedValue &=
                                Utils.writeFile(output_dir + ctx.getFilename() + "CdrAux.hpp",
                                    maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/TypesCdrAuxHeader.stg"), m_replace))
                        {
                            project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "CdrAux.hpp");
                            returnedValue &=
                                Utils.writeFile(output_dir + ctx.getFilename() + "CdrAux.ipp",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/TypesCdrAuxHeaderImpl.stg"), m_replace);
                        }
                    }
                    returnedValue &=
                        Utils.writeFile(output_dir + ctx.getFilename() + "PubSubTypes.hpp",
                                maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPubSubTypeHeader.stg"), m_replace);
                    project.addCommonIncludeFile(relative_dir + ctx.getFilename() + "PubSubTypes.hpp");
                    if (ctx.existsLastStructure())
                    {
                        m_atLeastOneStructure = true;
                        project.setHasStruct(true);

                        if (returnedValue &=
                                Utils.writeFile(output_dir + ctx.getFilename() + "PubSubTypes.cxx",
                                    maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPubSubTypeSource.stg"), m_replace))
                        {
                            project.addCommonSrcFile(relative_dir + ctx.getFilename() + "PubSubTypes.cxx");
                            if (m_python)
                            {
                                System.out.println("Generating Swig interface files...");
                                returnedValue &= Utils.writeFile(
                                        output_dir + ctx.getFilename() + "PubSubTypes.i",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPubSubTypeSwigInterface.stg"), m_replace);
                            }
                        }

                        if (m_exampleOption != null)
                        {
                            System.out.println("Generating Application files...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "Application.hpp",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSApplicationHeader.stg"), m_replace))
                            {
                                if (returnedValue =
                                        Utils.writeFile(output_dir + ctx.getFilename() + "Application.cxx",
                                            maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSApplicationSource.stg"), m_replace))
                                {
                                    project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "Application.hpp");
                                    project.addProjectSrcFile(relative_dir + ctx.getFilename() + "Application.cxx");
                                }
                            }

                            System.out.println("Generating PublisherApp files...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "PublisherApp.hpp",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPublisherHeader.stg"), m_replace))
                            {
                                if (returnedValue =
                                        Utils.writeFile(output_dir + ctx.getFilename() + "PublisherApp.cxx",
                                            maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPublisherSource.stg"), m_replace))
                                {
                                    project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "PublisherApp.hpp");
                                    project.addProjectSrcFile(relative_dir + ctx.getFilename() + "PublisherApp.cxx");
                                }
                            }

                            System.out.println("Generating SubscriberApp files...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "SubscriberApp.hpp",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSSubscriberHeader.stg"), m_replace))
                            {
                                if (returnedValue =
                                        Utils.writeFile(output_dir + ctx.getFilename() + "SubscriberApp.cxx",
                                            maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSSubscriberSource.stg"), m_replace))
                                {
                                    project.addProjectIncludeFile(relative_dir + ctx.getFilename() + "SubscriberApp.hpp");
                                    project.addProjectSrcFile(relative_dir + ctx.getFilename() + "SubscriberApp.cxx");
                                }
                            }

                            System.out.println("Generating main file...");
                            if (returnedValue =
                                    Utils.writeFile(output_dir + ctx.getFilename() + "main.cxx",
                                        maintemplates.getTemplate("com/eprosima/fastdds/idl/templates/DDSPubSubMain.stg"), m_replace))
                            {
                                project.addProjectSrcFile(relative_dir + ctx.getFilename() + "main.cxx");
                            }
                        }
                    }
                }
            }

            // Java support (Java classes and JNI code)
            if (returnedValue && m_languageOption == LANGUAGE.JAVA)
            {
                String outputDir = output_dir;

                // Make directories from package.
                if (!m_package.isEmpty())
                {
                    outputDir = output_dir + File.separator + m_package.replace('.', File.separatorChar);
                    File dirs = new File(outputDir);

                    if (!dirs.exists())
                    {
                        if (!dirs.mkdirs())
                        {
                            System.out.println(ColorMessage.error() + "Cannot create directories for Java packages.");
                            return null;
                        }
                    }
                }

                // Java classes.
                TypesGenerator typeGen = new TypesGenerator(tmanager, output_dir, m_replace);
                TypeCode.javapackage = m_package + (m_package.isEmpty() ? "" : ".");
                if (!typeGen.generate(ctx, outputDir + File.separator, m_package, ctx.getFilename(), null))
                {
                    System.out.println(ColorMessage.error() + "generating Java types");
                    return null;
                }

                if (ctx.existsLastStructure())
                {
                    System.out.println("Generating file " + output_dir + ctx.getFilename() + "PubSub.java");
                    if (!Utils.writeFile(outputDir + File.separator + ctx.getFilename() + "PubSub.java",
                            maintemplates.getTemplate("JavaSource"), m_replace))
                    {
                        return null;
                    }

                    // Call javah application for each structure.
                    if (!callJavah(idlFilename))
                    {
                        return null;
                    }
                }

                if (Utils.writeFile(output_dir + ctx.getFilename() + "PubSubJNII.hpp",
                        maintemplates.getTemplate("JNIHeader"),
                        m_replace))
                {
                    project.addJniIncludeFile(relative_dir + ctx.getFilename() + "PubSubJNII.hpp");
                }
                else
                {
                    return null;
                }

                TemplateST jnisourceTemplate = maintemplates.getTemplate("JNISource");
                if (Utils.writeFile(output_dir + ctx.getFilename() + "PubSubJNI.cxx", jnisourceTemplate, m_replace))
                {
                    project.addJniSrcFile(relative_dir + ctx.getFilename() + "PubSubJNI.cxx");
                }
                else
                {
                    return null;
                }
            }
        }

        return returnedValue ? project : null;
    }

    private boolean genSolution(
            Solution solution)
    {

        final String METHOD_NAME = "genSolution";
        boolean returnedValue = true;
        if (m_atLeastOneStructure == true)
        {
            if (m_exampleOption != null)
            {
                System.out.println("Generating solution for arch " + m_exampleOption + "...");

                if (m_exampleOption.equals("CMake") || m_test)
                {
                    System.out.println("Generating CMakeLists solution");
                    returnedValue = genCMakeLists(solution);
                }
                else if (m_exampleOption.substring(3, 6).equals("Win"))
                {
                    System.out.println("Generating Windows solution");
                    if (m_exampleOption.startsWith("i86"))
                    {
                        returnedValue = genVS(solution, null, "16", "142");
                    }
                    else if (m_exampleOption.startsWith("x64"))
                    {
                        for (int index = 0; index < m_vsconfigurations.length; index++)
                        {
                            m_vsconfigurations[index].setPlatform("x64");
                        }
                        returnedValue = genVS(solution, "x64", "16", "142");
                    }
                    else
                    {
                        returnedValue = false;
                    }
                }
            }
        }
        else
        {
            System.out.println(
                ColorMessage.warning() +
                "No structure found in any of the provided IDL; no example files have been generated");
        }

        return returnedValue;
    }

    private boolean genVS(
            Solution solution,
            String arch,
            String vsVersion,
            String toolset)
    {

        final String METHOD_NAME = "genVS";
        boolean returnedValue = false;

        STGroupFile vsTemplates = new STGroupFile("com/eprosima/fastdds/idl/templates/VS.stg", '$', '$');

        if (vsTemplates != null)
        {
            ST tsolution = vsTemplates.getInstanceOf("solution");

            returnedValue = true;

            for (int count = 0; returnedValue && (count < solution.getProjects().size()); ++count)
            {
                ST tproject = vsTemplates.getInstanceOf("project");
                ST tprojectFiles = vsTemplates.getInstanceOf("projectFiles");
                ST tprojectPubSub = vsTemplates.getInstanceOf("projectPubSub");
                ST tprojectFilesPubSub = vsTemplates.getInstanceOf("projectFilesPubSub");
                ST tprojectJNI = null;
                ST tprojectFilesJNI = null;
                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tprojectJNI = vsTemplates.getInstanceOf("projectJNI");
                    tprojectFilesJNI = vsTemplates.getInstanceOf("projectFilesJNI");
                }

                Project project = (Project) solution.getProjects().get(count);

                tproject.add("solution", solution);
                tproject.add("project", project);
                tproject.add("example", m_exampleOption);
                tproject.add("vsVersion", vsVersion);
                tproject.add("toolset", toolset);

                tprojectFiles.add("project", project);
                tprojectFiles.add("vsVersion", vsVersion);

                tprojectPubSub.add("solution", solution);
                tprojectPubSub.add("project", project);
                tprojectPubSub.add("example", m_exampleOption);
                tprojectPubSub.add("vsVersion", vsVersion);
                tprojectPubSub.add("toolset", toolset);

                tprojectFilesPubSub.add("project", project);
                tprojectFilesPubSub.add("vsVersion", vsVersion);

                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tprojectJNI.add("solution", solution);
                    tprojectJNI.add("project", project);
                    tprojectJNI.add("example", m_exampleOption);
                    tprojectJNI.add("vsVersion", vsVersion);
                    tprojectJNI.add("toolset", toolset);

                    tprojectFilesJNI.add("project", project);
                    tprojectFilesJNI.add("vsVersion", vsVersion);
                }

                for (int index = 0; index < m_vsconfigurations.length; index++)
                {
                    tproject.add("configurations", m_vsconfigurations[index]);
                    tprojectPubSub.add("configurations", m_vsconfigurations[index]);
                    if (m_languageOption == LANGUAGE.JAVA)
                    {
                        tprojectJNI.add("configurations", m_vsconfigurations[index]);
                    }
                }

                if (returnedValue =
                        Utils.writeFile(m_outputDir + project.getName() + "Types-" + m_exampleOption + ".vcxproj",
                        tproject, m_replace))
                {
                    if (returnedValue =
                            Utils.writeFile(m_outputDir + project.getName() + "Types-" + m_exampleOption +
                            ".vcxproj.filters", tprojectFiles, m_replace))
                    {
                        if (project.getHasStruct())
                        {
                            if (returnedValue =
                                    Utils.writeFile(m_outputDir + project.getName() + "PublisherSubscriber-" +
                                    m_exampleOption + ".vcxproj", tprojectPubSub, m_replace))
                            {
                                returnedValue = Utils.writeFile(
                                    m_outputDir + project.getName() + "PublisherSubscriber-" + m_exampleOption + ".vcxproj.filters", tprojectFilesPubSub,
                                    m_replace);
                            }
                        }
                    }
                }

                if (returnedValue && m_languageOption == LANGUAGE.JAVA)
                {
                    if (returnedValue =
                            Utils.writeFile(m_outputDir + project.getName() + "PubSubJNI-" + m_exampleOption +
                            ".vcxproj", tprojectJNI, m_replace))
                    {
                        returnedValue = Utils.writeFile(
                            m_outputDir + project.getName() + "PubSubJNI-" + m_exampleOption + ".vcxproj.filters", tprojectFilesJNI,
                            m_replace);
                    }
                }
            }

            if (returnedValue)
            {
                tsolution.add("solution", solution);
                tsolution.add("example", m_exampleOption);

                // Project configurations
                for (int index = 0; index < m_vsconfigurations.length; index++)
                {
                    tsolution.add("configurations", m_vsconfigurations[index]);
                }

                if (m_languageOption == LANGUAGE.JAVA)
                {
                    tsolution.add("generateJava", true);
                }

                String vsVersion_sol = "2019";
                tsolution.add("vsVersion", vsVersion_sol);

                returnedValue = Utils.writeFile(m_outputDir + "solution-" + m_exampleOption + ".sln", tsolution,
                                m_replace);
            }

        }
        else
        {
            System.out.println("ERROR<" + METHOD_NAME + ">: Cannot load the template group VS");
        }

        return returnedValue;
    }

    private boolean genCMakeLists(
            Solution solution)
    {

        boolean returnedValue = false;
        ST cmake = null;

        STGroupFile cmakeTemplates = new STGroupFile("com/eprosima/fastdds/idl/templates/CMakeLists.stg", '$', '$');

        if (cmakeTemplates != null)
        {
            cmake = cmakeTemplates.getInstanceOf("cmakelists");

            cmake.add("solution", solution);
            cmake.add("test", m_test);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", cmake, m_replace);
        }
        return returnedValue;
    }

    private boolean genSwigCMake(
            Solution solution)
    {

        boolean returnedValue = false;
        ST swig = null;

        STGroupFile swigTemplates = new STGroupFile("com/eprosima/fastdds/idl/templates/SwigCMake.stg", '$', '$');
        if (swigTemplates != null)
        {
            swig = swigTemplates.getInstanceOf("swig_cmake");

            swig.add("solution", solution);

            returnedValue = Utils.writeFile(m_outputDir + "CMakeLists.txt", swig, m_replace);

        }
        return returnedValue;
    }

    String callPreprocessor(
            String idlFilename)
    {
        final String METHOD_NAME = "callPreprocessor";

        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String [] lineCommandArray = null;
        String outputfile = Util.getIDLFileOnly(idlFilename) + ".cc";
        int exitVal = -1;
        OutputStream of = null;

        // Use temp directory.
        if (m_tempDir != null)
        {
            outputfile = m_tempDir + outputfile;
        }

        if (m_os.contains("Windows"))
        {
            try
            {
                of = new FileOutputStream(outputfile);
            }
            catch (FileNotFoundException ex)
            {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot open file " + outputfile);
                return null;
            }
        }

        // Set the preprocessor path
        String ppPath = m_ppPath;

        if (ppPath == null)
        {
            if (m_os.contains("Windows"))
            {
                ppPath = "cl.exe";
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                ppPath = "cpp";
            }
        }

        // Add command
        lineCommand.add(ppPath);

        // Add the include paths given as parameters.
        for (int i = 0; i < m_includePaths.size(); ++i)
        {
            if (m_os.contains("Windows"))
            {
                lineCommand.add(((String) m_includePaths.get(i)).replaceFirst("^-I", "/I"));
            }
            else if (m_os.contains("Linux") || m_os.contains("Mac"))
            {
                lineCommand.add(m_includePaths.get(i));
            }
        }

        if (m_os.contains("Windows"))
        {
            lineCommand.add("/E");
            lineCommand.add("/C");
        }

        // Add input file.
        lineCommand.add(idlFilename);

        if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            lineCommand.add(outputfile);
        }

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, of, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception e)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the preprocessor. Reason: " + e.getMessage());
            return null;
        }

        if (of != null)
        {
            try
            {
                of.close();
            }
            catch (IOException e)
            {
                System.out.println(ColorMessage.error(METHOD_NAME) + "Cannot close file " + outputfile);
            }

        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "Preprocessor return an error " + exitVal);
            return null;
        }

        return outputfile;
    }

    boolean callJavah(
            String idlFilename)
    {
        final String METHOD_NAME = "calljavah";
        // Set line command.
        ArrayList<String> lineCommand = new ArrayList<String>();
        String[] lineCommandArray = null;
        String fileDir = Util.getIDLFileDirectoryOnly(idlFilename);
        String javafile = (m_outputDir != null ? m_outputDir : "") +
                (!m_package.isEmpty() ? m_package.replace('.', File.separatorChar) + File.separator : "") +
                Util.getIDLFileNameOnly(idlFilename) + "PubSub.java";
        String headerfile = m_outputDir + Util.getIDLFileNameOnly(idlFilename) + "PubSubJNI.hpp";
        int exitVal = -1;
        String javac = null;
        String javah = null;

        // First call javac
        if (m_os.contains("Windows"))
        {
            javac = "javac.exe";
        }
        else if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javac = "javac";
        }

        // Add command
        lineCommand.add(javac);
        if (m_tempDir != null)
        {
            lineCommand.add("-d");
            lineCommand.add(m_tempDir);
        }

        if ( fileDir != null && !fileDir.isEmpty())
        {
            lineCommand.add("-sourcepath");
            lineCommand.add(m_outputDir);
        }

        lineCommand.add(javafile);

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the javac application. Reason: " + ex.getMessage());
            return false;
        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javac application return an error " + exitVal);
            return false;
        }

        lineCommand = new ArrayList<String>();

        if (m_os.contains("Windows"))
        {
            javah = "javah.exe";
        }
        else if (m_os.contains("Linux") || m_os.contains("Mac"))
        {
            javah = "javah";
        }

        // Add command
        lineCommand.add(javah);
        lineCommand.add("-jni");
        if (m_tempDir != null)
        {
            lineCommand.add("-cp");
            lineCommand.add(m_tempDir);
        }
        lineCommand.add("-o");
        lineCommand.add(headerfile);
        lineCommand.add((!m_package.isEmpty() ? m_package + "." : "") +
                Util.getIDLFileNameOnly(idlFilename) + "PubSub");

        lineCommandArray = new String[lineCommand.size()];
        lineCommandArray = (String[])lineCommand.toArray(lineCommandArray);

        try
        {
            Process preprocessor = Runtime.getRuntime().exec(lineCommandArray);
            ProcessOutput errorOutput = new ProcessOutput(preprocessor.getErrorStream(), "ERROR", false, null, true);
            ProcessOutput normalOutput = new ProcessOutput(preprocessor.getInputStream(), "OUTPUT", false, null, true);
            errorOutput.start();
            normalOutput.start();
            exitVal = preprocessor.waitFor();
            errorOutput.join();
            normalOutput.join();
        }
        catch (Exception ex)
        {
            System.out.println(ColorMessage.error(
                        METHOD_NAME) + "Cannot execute the javah application. Reason: " + ex.getMessage());
            return false;
        }

        if (exitVal != 0)
        {
            System.out.println(ColorMessage.error(METHOD_NAME) + "javah application return an error " + exitVal);
            return false;
        }

        return true;
    }

    /*
     * ----------------------------------------------------------------------------------------
     *
     * Main entry point
     */

    public static void main(
            String[] args)
    {
        ColorMessage.load();

        if (loadPlatforms())
        {

            try
            {

                fastddsgen main = new fastddsgen(args);
                if (main.execute())
                {
                    System.exit(0);
                }

            }
            catch (BadArgumentException e)
            {

                System.out.println(ColorMessage.error("BadArgumentException") + e.getMessage());
                printHelp();

            }

        }

        System.exit(-1);
    }

}

class ProcessOutput extends Thread
{
    InputStream is = null;
    OutputStream of = null;
    String type;
    boolean m_check_failures;
    boolean m_found_error = false;
    final String clLine = "#line";
    boolean m_printLine = false;

    ProcessOutput(
            InputStream is,
            String type,
            boolean check_failures,
            OutputStream of,
            boolean printLine)
    {
        this.is = is;
        this.type = type;
        m_check_failures = check_failures;
        this.of = of;
        m_printLine = printLine;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (of == null)
                {
                    if (m_printLine)
                    {
                        System.out.println(line);
                    }
                }
                else
                {
                    // Substitute "\\" added by cl.exe for "\"
                    if (line.startsWith(clLine))
                    {
                        line = "#" + line.substring(clLine.length());
                        int count = 0;
                        while ((count = line.indexOf("\\\\")) != -1)
                        {
                            line = line.substring(0, count) + "\\" + line.substring(count + 2);
                        }
                    }

                    of.write(line.getBytes());
                    of.write('\n');
                }

                if (m_check_failures)
                {
                    if (line.startsWith("Done (failures)"))
                    {
                        m_found_error = true;
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    boolean getFoundError()
    {
        return m_found_error;
    }

}
