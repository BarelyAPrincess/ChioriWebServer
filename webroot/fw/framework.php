<?
function getFramework()
{
	return java_bean("framework");
}

if ( empty( $_REQUEST["page"] ) )
{
	echo getFramework()->error( 502, "Internal Server Error" );
}
else
{
	echo getFramework()->initalize( $_SERVER );
	
	ob_start(); // Start Output Buffer Session.
	include($_REQUEST["page"]); // Include requested file to to be captured by ob.
	$result = ob_get_contents(); // Retreive output buffer contents.
	ob_end_clean(); // Erase ob contents.
	
	echo getFramework()->pageLoad( $result );
}
?>