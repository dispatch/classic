Databinder: a simple bridge from Wicket to Hibernate

Please see the Databinder Web site for documentation and usage examples:
http://databinder.net/


Version History

0.8	Many base classes in the toolkit have been refactored to be more 
	flexible, including DataApplication, AuthDataApplication, 
	DataRequestCycle, and AuthDataRequestCycle. Existing applications 
	that override default functionality in base classes may need to 
	make minor changes to compile against this version.

	The Hibernate session factory is now easily accessible from
	outside a Wicket request cycle, in DatabinderStaticService. An
	exception is thrown if the session factory is not initialized,
	provoked by the common error of overriding DataApplication.init()
	without calling its super-implementation.
	
	The focusable form input from the recipe book example is now a
	toolkit component, FocusableTextField. SearchPanel now has a 
	submit button for better compatibility with older browsers (and 
	users). AjaxOnKeyPause handles backspace correctly with IE, 
	and it no longer needs to be the first Ajax behavior on a page.
	DataRegistrationPanel uses a PasswordInputValidator to avoid 
	exposing passwords.

	The JavaScript and stylesheet link components now extend 
	appropriate resource reference classes in the Wicket hierarchy. 
	DataPage's default stylesheet has been improved to follow form 
	label guidelines, and feedback messages have a new appearance.

	Wicket and Hibernate Annotations dependencies have been updated to
	1.2.2 and 3.2.0 RC2 respectively. MySQL connector (in data-app and
	examples) is at version 5.0.3.

0.7	Wicket DataView support arrives in this version. Though it has 
	always been possible to write your own IDataProvider, Databinder
	now includes one for you. See the baseball players example.

	The provided User class has been renamed DataUser to avoid that 
	reserved table name in some databases. You will need to update 
	any code referring to this class. Subclassing of DataSignInPage
	has been streamlined.

	The data-app archetype now includes a sample entity. In web.xml
	it uses the new RedirectFilter class to send context-root 
	requests to the Wicket servlet. All Databinder projects using 
	the now-deprecated RedirectServlet are encouraged to switch to 
	RedirectFilter as it permits static content under the 
	context-root.

	RenderedLabel now loads its image when its markup is rendered, 
	so that drawing the image when it is later requested will 
	not require a model to be reattached.

	PageStyleLink and PropertyListModel, which have been deprecated
	for some time, are now out of the library. The StyleLink and 
	ScriptLink classes have been rewritten to fall under the 
	PackagedResourceReference component, requiring the removal of 
	their constructors that took the Class object as an IModel. 
	Please use these classes with a simple Class object.

0.6	This version adds integration with the wicket-auth-roles package
	via the AuthDataApplication superclass. Default implementation
	classes for users and roles are built in, along with basic sign
	in, registration, and remember-me functionality. The recipe book
	example demonstrates selective authentication for data editing.

	A new SublistProjectionModel class breaks lists into chunked or
	transposed sublists for common rendering styles (see bookmarks). 
	A RenderedLabel component renders its model into an image using
	any font available to the JVM, as demonstrated in the new
	graffiti example. AjaxOnKeyPausedUpdater triggers Ajax updates
	when input pauses in a TextField or TextArea and replaces the
	old "every key" behavior of SearchPanel. Added support for
	Hibernate criteria queries to object and list models.

	Changed structure of data-app archetype to be compatible with
	Maven 2.0.4. HTML and property resources are now stored
	alongside Java sources by default. Incremented versions of
	several dependencies, including Wicket to 1.2.1 which now
	enforces that session-stored objects be Serializable by default. 

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
