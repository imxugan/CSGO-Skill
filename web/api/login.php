<?php

require_once('openid.php');
require_once('setup.php');
require_once('dbInf.php');

try {
    if (isset($_GET["app"])) {
        $openid = new LightOpenID("http://api.flare-esports.net/login?app");
    } else {
        $openid = new LightOpenID("http://www.flare-esports.net/login");
    }

	if(!$openid->mode) {
		$openid->identity = 'http://steamcommunity.com/openid';
		header('Location: ' . $openid->authUrl());
	} elseif ($openid->mode == 'cancel') {
        // No need to log when a user cancels authentication.
        consoleExit("{\"success\":false,\"error\":\"1218\"}");
	} else {
		if($openid->validate()) {
			$id = $openid->identity;
			$ptn = "/^http:\/\/steamcommunity\.com\/openid\/id\/(7[0-9]{15,25}+)$/";
			preg_match($ptn, $id, $matches);
            $steamID = $matches[1];
            $conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
            if ($conn->connect_error) {
                error_log("1212 - Failed to connect to MySQL Database '" . FLAREDB .
                "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
                consoleExit("{\"success\":false,\"error\":\"1212\"}");
            }
            $query = "SELECT `secret` FROM `Players_01` WHERE `steamid`=\"" . $conn->real_escape_string($steamID) .
                     "\" AND `verified` = 1";
            $result = $conn->query($query);
            if ($result->num_rows === 0) {
                $conn->close();
                // This, without anything but the $steamID, will check if the account is valid, reserve the row, and return true.
                require_once("makeAccount.php");
            }
            $result = $result->fetch_assoc()["secret"];
            $conn->close();
            consoleExit("{\"success\":true,\"secret\":\"" . $result . "\"}");
		} else {
            error_log("1219 - OpenID failed to validate. Reason unknown.");
			consoleExit("{\"success\":false,\"error\":\"1219\"}");
		}
	}
} catch(ErrorException $e) {
    error_log("1220 - ErrorException occured: " . $e->getMessage());
	consoleExit("{\"success\":false,\"error\":\"1220\"}");
}
