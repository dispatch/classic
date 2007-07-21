def child_artifact(child_spec, parent_artifact, path)
	parent_artifact.invoke
	artifact(child_spec) do |task|
		file_name = File.basename(path) 
		dest_path = File.dirname(task.name) 
		unz = Unzip.new(dest_path => parent_artifact)
		unz.from_path(File.dirname(path)).include(file_name)
		unz.extract()
		mv(File.join(dest_path, file_name), task.name) 
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

WICKET_SELF = group("wicket", "wicket-auth-roles", "wicket-extensions", :under=>"org.apache.wicket", :version=>"1.3.0-beta2")
WICKET=[WICKET_SELF, "commons-collections:commons-collections:jar:2.1.1","commons-logging:commons-logging:jar:1.0.4"]

HB_CORE_ZIP=download(artifact("org.hibernate:hibernate:zip:3.2.4.sp1")=>"http://dl.sourceforge.net/sourceforge/hibernate/hibernate-3.2.4.sp1.zip")
HIBERNATE_CORE = child_artifact("org.hibernate:hibernate:jar:3.2.4.sp1", HB_CORE_ZIP, "hibernate-3.2/hibernate3.jar")
HIBERNATE_SELF = [HIBERNATE_CORE,"org.hibernate:hibernate-annotations:jar:3.3.0.ga", "org.hibernate:hibernate-commons-annotations:jar:3.3.0.ga"]
JTA = child_artifact("javax.transaction:jta:jar:1.0.1B", HB_CORE_ZIP, "hibernate-3.2/lib/jta.jar")
CGLIB = child_artifact("cglib:cglib:jar:2.1_3", HB_CORE_ZIP, "hibernate-3.2/lib/cglib-2.1.3.jar")
EHCACHE=child_artifact("net.sf.ehcache:ehcache:jar:1.2.3", HB_CORE_ZIP, "hibernate-3.2/lib/ehcache-1.2.3.jar")
HIBERNATE=[HIBERNATE_SELF, JTA, EHCACHE, CGLIB, "javax.persistence:persistence-api:jar:1.0", "dom4j:dom4j:jar:1.6.1", "asm:asm-attrs:jar:1.5.3", "asm:asm:jar:1.5.3", "antlr:antlr:jar:2.7.6"]

DATABINDER_COMPONENTS="net.databinder:databinder-components:jar:1.1-SNAPSHOT"
DATABINDER_SELF=[DATABINDER_COMPONENTS, group("databinder","databinder-dispatch", "databinder-auth-components", "databinder-models", :under => "net.databinder", :version => "1.1-SNAPSHOT")]
XML_RPC = ["org.apache.ws.commons:ws-commons-util:jar:1.0.1","org.apache.xmlrpc:xmlrpc-client:jar:3.0","org.apache.xmlrpc:xmlrpc-common:jar:3.0", "commons-httpclient:commons-httpclient:jar:3.0.1", "commons-codec:commons-codec:jar:1.2"]
DATABINDER=[DATABINDER_SELF, WICKET, HIBERNATE, XML_RPC]