Databinder: a simple bridge from Wicket to Hibernate

Please see the Web site for documentation and usage examples:
http://databinder.net


Version History

0.5	Trimming and meshing for Wicket 1.2, including deprecation of
	PropertyListView and removal of application.properties from
	the data-app archetype. Recognition of the wicket.configuration
	JVM parameter, in addition to net.databinder.configuration. Ajax
	convenience Wrapper class added.

	Hibernate dependency updated to 3.2.0 cr2 and Annotations 
	3.2.0 cr1. Fixed bug leading to unknown entity exceptions caused
	by Hibernate proxy classes. Added trigger for Hibernate 
	initialization on startup rather than the first page request.
	HibernateObjectModel now loads objects upon  instantiation so 
	that object not found (and other) exceptions can be easily 
	caught.

	New overridable isCookielessSupported() method of DataApplication
	allows URL rewriting to be disabled in Wicket so that search 
	engines can crawl bookmarkable links without jsessionid URL
	parameters.

	RedirectServlet provided for easily sending requests from the
	Web root to WicketServlet. Added configuration for this, as
	well as the databinder.net repository, to the data-app archetype.


0.4	Integration with Wicket 1.2 beta 2 and exploitation of its 
	built-in Ajax capabilities. Databinder's new SearchPanel component 
	provides ready-made live search functionality, and all three 
	example applications have been updated to use Ajax as 
	appropriate (or in some cases, just to show how it works).

	HibernateObjectModel can now be initialized with a query for the 
	not-so-special case of eager fetch joins and scalar results. 

	In existing Databinder applications, subclasses of DataApplication 
	must change the visibility of getHomePage() from protected to 
	public, as Wicket has added exactly that method to its base 
	Application class. Other changes to your applications may be 
	necessary; see Wicket's 1.2 migration guide for more information:

	http://wicket-wiki.org.uk/wiki/index.php/Migrate-1.2

0.3	New helper components: TextileLabel and DateLabel. The former 
	renders its content using JTextile (modified for Java's built-in 
	regular expression processor) and the latter using 
	SimpleDateFormat with a supplied format string. A new recipe book 
	example demonstrates the Textile support, as well as some basic  
	Panel use, embedded Hibernate objects, and JavaScript.

	Transparent version awareness added to DataForm, following a 
	method suggested in Martijn Dashorst's weblog. Attempting to 
	overwrite another user's unseen changes to an object now results 
	in a validation error.

	Dependencies have been updated to Hibernate 3.1.2 for the 
	library, and jetty6beta9 in the archetype. Fixed a bug in the 
	DataForm(id, model) constructor; it now wraps the model in a 
	compound model as indicated in the comments. PageStyleLink has 
	been renamed StyleLink to reflect its applicability to any 
	component, and a new ScriptLink component helps attach JavaScript.

0.2	Dependencies are updated to Hibernate 3.1.1, Annotations 
	3.1beta8,and Wicket 1.1.1. DataApplication.getSessionFactory() 
	is no longer final (nor overridden), and c3p0 pooling defaults to 
	testing for closed connections. Wicket's log4j dependency is now
	excluded; add it to your <<<pom.xml>>> if you use it.

	New component WebLink along with a URLConverter provide better 
	support for linking outside of Wicket. PageStyleLink makes it easy 
	to add a stylesheet specific to a page or component

	DataRequestCycle now begins a Hibernate transaction when creating 
	a Hibernate session, and DataForm commits that transaction in 
	onSubmit().

	Improved Phone directory example applies optimistic locking, 
	and its ListAndEdit page uses a compact SearchFilter 
	implementation. New Bookmark example application is very short, 
	simple, and intended for Wicket novices.

0.1	Support for Hibernate 3.1, Hibernate Annotations 3.1beta7, and
	Wicket 1.1. This is the first public release of Databinder. All
	that is known for sure is that it can support the small Phone
	directory example application (available on Web site).




--------------------------------------------------------------------------
Nathan Hamblen / nathan@technically.us
