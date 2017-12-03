<?php

require_once('openid');
require_once('setup');

try {
	$openid = new LightOpenID($steamauth['domain']);

	if(!$openid->mode) {
		$openid->identity = 'http://steamcommunity.com/openid';
        /**
         * Set return URL to the currently executing script, this allows us to
         * simply require_once() this file when we want the user to login, and
         * gracefully redirected back to the original calling script.
         *
         * The Application will call this script directly, allowing super-dooper
         * easy redirection and to pull data in a more effective and secure way.
         */
        $openid->returnUrl = $_SERVER["PHP_SELF"];
		header('Location: ' . $openid->authUrl());
	} elseif ($openid->mode == 'cancel') {
        // No need to log when a user cancels authentication.
        consoleExit("{\"success\":false,\"error\":\"1218\"}");
	} else {
		if($openid->validate()) {
			$id = $openid->identity;
			$ptn = "/^http:\/\/steamcommunity\.com\/openid\/id\/(7[0-9]{15,25}+)$/";
			preg_match($ptn, $id, $matches);
            $steamID = $mathes[1];
            $conn = mysqli_connect($server, $username, $password, $flaredb);
            if ($conn->connect_error) {
                error_log("1212 - Failed to connect to MySQL Database '" . $flaredb .
                "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
                consoleExit("{\"success\":false,\"error\":\"1212\"}");
            }
            $query = "SELECT `secret` FROM `Players_01` WHERE `steamid`=\"" . $conn->real_escape_string($steamID) .
                     "\" AND `verified` = 1";
            $result = $conn->query($query);
            if ($result->num_rows === 0) {
                $conn->close();
                // This, without anything but the $steamID, will check if the account is valid, reserve the row, and return true.
                require_once("makeAccount");
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
