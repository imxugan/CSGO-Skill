<?php

require_once("setup.php");

if (count($_POST) !== 4) {
    consoleExit("{\"success\":false,\"error\":\"1414\"}");
}
if (strlen(http_build_query($_POST)) > 200) {
    consoleExit("{\"success\":false,\"error\":\"1428\"}");
}
$expected = array("steamid", "email", "name", "verify");
foreach ($expected as $f) {
    if (!array_key_exists($f, $_POST)) {
        consoleExit("{\"success\":false,\"error\":\"1414\"}");
    }
}

$email = $_POST["email"];
$name = $_POST["name"];
$steamID = $_POST["steamid"];
$verify = $_POST["verify"];
$create = true;

require_once("makeAccount.php"); // This will do the rest for us

?>
