To create Eclipse launcher for debugging must specify the following:
-Dosgi.framework.extensions=org.eclipse.fx.osgi

For Java 11 and above, must also include:
-Defxclipse.java-modules.dir=<path to JavaFx SDK install lib directory - see https://openjfx.io/ >

For Java 8, need to not use GTK3 by environment variable:
SWT_GTK3=0

Useful for JavaFx debugging:
-Defxclipse.osgi.hook.debug=true 