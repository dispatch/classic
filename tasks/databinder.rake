repositories.local = ENV['M2_REPO'] if ENV['M2_REPO']

def child_artifact(child_spec, parent_artifact, path)
  parent_artifact.invoke
  artifact(child_spec) do |task|
    file_name = File.basename(path) 
    dest_path = File.dirname(task.name) 
    unz = Unzip.new(dest_path => parent_artifact)
    unz.from_path(File.dirname(path)).include(file_name)
    unz.extract()
    tgt = (File.join(dest_path, file_name))
    mv(tgt, task.name) if not File.exist? task.name
  end
end

def dep_preview(path, parent_spec)
  file_name = File.basename(path)
  parent_artifact = artifact(parent_spec)
  file("dep_preview/" + file_name => parent_artifact) do |task|
    dest_path = File.dirname(task.name) 
    unz = Unzip.new(dest_path => parent_artifact)
    unz.from_path(File.dirname(path)).include(file_name)
    unz.extract()
  end
end

module SoftFail
  def fail_download(remote_uris)
    puts "Can't find javadoc #{to_spec}\n"
  end
end

LIB_DOCS='lib_docs'
def doc_tasks(jar_spec)
  artifacts(jar_spec).map do |a|
    doc_a = artifact(a.to_hash.merge({:classifier=>'javadoc'}))
    doc_a.extend SoftFail
    file(_(LIB_DOCS) + '/' + a.to_spec => doc_a) do |task|
      unzip(task.name => doc_a) if File.exist? doc_a.name
    end
  end
end

def embed_server

  compile.with JETTY
  scalac.with JETTY if defined? scalac
  test.with JDK_LOG

  def scala_libs()
    FileList["#{ENV["SCALA_HOME"]}/lib/*"]
  end

  def java_runner(cp, params = [], main_class = 'net.databinder.web.DataServer')
    params << "-Dmail.smtp.host=$SMTP_HOST" if ENV["SMTP_HOST"]
    params << '-Djetty.port=' + ENV['JETTY_PORT'] if ENV['JETTY_PORT']
    params << '-Djetty.ajp.port=' + ENV['JETTY_AJP_PORT'] if ENV['JETTY_AJP_PORT']
    params << '-Djetty.contextPath=' + ENV['JETTY_CONTEXT'] if ENV['JETTY_CONTEXT']

    cp += [compile.target.to_s]
    if defined? scalac then
      cp += scalac.classpath + scala_libs
    end
    cp

    ENV['JAVA_HOME'] + "/bin/java $JAVA_OPTIONS " << params.join(" ") << ' -cp ' << cp.uniq.join(":") << ' ' << main_class
  end

  def proj_sys(cmd)
    wd = Dir.pwd
    Dir.chdir _('.')
    system cmd
    Dir.chdir wd
  end

  def rebel_params()
    if ENV["JAVA_REBEL"]
    	["-noverify", "-javaagent:$JAVA_REBEL"]
   	else [] end
  end
  
  task :run => :build do
    proj_sys java_runner(test.classpath, rebel_params)
  end

  task :play => :build do
    raise('sorry, a SCALA_HOME is required to play') if not ENV['SCALA_HOME']
    proj_sys java_runner(test.classpath + scala_libs, rebel_params, 'scala.tools.nsc.MainGenericRunner')
  end

  def pid_f() _("server.pid") end

  def pid() File.exist?(pid_f) && IO.read(pid_f).to_i end

  task :start => :package do
    cp = compile.classpath + artifacts(LOG4J).map { |a| a.name }
    proj_sys 'nohup ' << java_runner(cp) << '>/dev/null &\echo $! > ' << pid_f
    puts "started server pid: " << pid().to_s
  end

  task :stop do
    if pid
      begin
        Process.kill("TERM", pid)
      rescue Errno::ESRCH
        puts "server not running at pid: #{pid}; removing record"
      end
      rm pid_f
    else
      puts "no server id on record"
    end
  end
end

WICKET_SELF = group("wicket", "wicket-auth-roles", "wicket-extensions", :under=>"org.apache.wicket", :version=>"1.3.0")
WICKET=[WICKET_SELF, "commons-collections:commons-collections:jar:2.1.1","org.slf4j:slf4j-api:jar:1.4.2"]
MAIL = ["javax.mail:mail:jar:1.4", "javax.activation:activation:jar:1.1"]
LOG4J = ["org.slf4j:slf4j-log4j12:jar:1.4.2","log4j:log4j:jar:1.2.14", MAIL]
JDK_LOG = ["org.slf4j:slf4j-jdk14:jar:1.4.2"]

HB_CORE_ZIP=download(artifact("org.hibernate:hibernate:zip:3.2.5.ga")=>"http://easynews.dl.sourceforge.net/sourceforge/hibernate/hibernate-3.2.5.ga.zip")
HIBERNATE_CORE = child_artifact("org.hibernate:hibernate:jar:3.2.5.ga", HB_CORE_ZIP, "hibernate-3.2/hibernate3.jar")
HIBERNATE_SELF = [HIBERNATE_CORE,"org.hibernate:hibernate-annotations:jar:3.3.0.ga", "org.hibernate:hibernate-commons-annotations:jar:3.3.0.ga"]
JTA = child_artifact("javax.transaction:jta:jar:1.0.1B", HB_CORE_ZIP, "hibernate-3.2/lib/jta.jar")
CGLIB = child_artifact("cglib:cglib:jar:2.1_3", HB_CORE_ZIP, "hibernate-3.2/lib/cglib-2.1.3.jar")
EHCACHE=child_artifact("net.sf.ehcache:ehcache:jar:1.2.3", HB_CORE_ZIP, "hibernate-3.2/lib/ehcache-1.2.3.jar")
C3P0='c3p0:c3p0:jar:0.9.0.4'
HIBERNATE=[HIBERNATE_SELF, JTA, EHCACHE, CGLIB, "javax.persistence:persistence-api:jar:1.0", "dom4j:dom4j:jar:1.6.1", "asm:asm-attrs:jar:1.5.3", "asm:asm:jar:1.5.3", "antlr:antlr:jar:2.7.6", "commons-logging:commons-logging:jar:1.0.4"]

DATABINDER_COMPONENTS="net.databinder:databinder-components:jar:1.1-SNAPSHOT"
DATABINDER_SELF=[DATABINDER_COMPONENTS, group("databinder","databinder-dispatch", "databinder-auth-components", "databinder-models", :under => "net.databinder", :version => "1.1-SNAPSHOT")]
XML_RPC = ["org.apache.ws.commons:ws-commons-util:jar:1.0.1","org.apache.xmlrpc:xmlrpc-client:jar:3.0","org.apache.xmlrpc:xmlrpc-common:jar:3.0", "commons-httpclient:commons-httpclient:jar:3.0.1", "commons-codec:commons-codec:jar:1.2"]
DATABINDER=[DATABINDER_SELF, WICKET, HIBERNATE, XML_RPC, C3P0]

JETTY = group('jetty','jetty-util','jetty-ajp', 'servlet-api-2.5', :under=>'org.mortbay.jetty', :version=>'6.1.5')
