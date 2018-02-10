<?php

require_once('openid.php');
require_once('setup.php');

try {
    if (isset($_GET["app"])) {
        $app = true;
        $openid = new LightOpenID("http://api.csgo-skill.com/login?app");
    } else {
        $app = false;
        $openid = new LightOpenID("http://www.csgo-skill.com/login");
    }

	if(!$openid->mode) {
		$openid->identity = 'http://steamcommunity.com/openid';
		header('Location: ' . $openid->authUrl());
	} elseif ($openid->mode == 'cancel') {
        // No need to log when a user cancels authentication.
        $msg = "{\"success\":false,\"error\":\"1218\"}";
        if ($app){ consoleExit($msg); }
        else { exit($msg); }
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
                $msg = "{\"success\":false,\"error\":\"1212\"}";
                if ($app){ consoleExit($msg); }
                else { exit($msg); }
            }
            $query = "SELECT `secret`, `verified`, `status`, `email` FROM `Players_01` WHERE `steamid`=\"" . $conn->real_escape_string($steamID) . "\"";
            $result = $conn->query($query);
            if ($result->num_rows === 0) {
                $conn->close();
                // This, without anything but the $steamID, will check if the account is valid, reserve the row, and return true.
                require_once("makeAccount.php");
            }
            $result = $result->fetch_assoc();
            $status = json_decode($result["status"]);
            $conn->close();
            if ($result["verified"] == 0) {
                if (isset($status->link) && isset($status->link->action) && $status->link->action == "verify") {
                    $msg = "{\"success\":true,\"email\":\"" . $result["email"] . "\",\"steamid\":\"" . $steamID . "\",\"secret\":\"" . $result["secret"] . "\"}";
                } else {
                    // Assume retry for signup
                    require_once("makeAccount.php");
                }
            }
            $msg = "{\"success\":true,\"steamid\":\"" . $steamID . "\",\"secret\":\"" . $result["secret"] . "\"}";
            if ($app){ consoleExit($msg); }
            else { exit($msg); }
		} else {
            error_log("1219 - OpenID failed to validate. Reason unknown.");
			$msg = "{\"success\":false,\"error\":\"1219\"}";
            if ($app){ consoleExit($msg); }
            else { exit($msg); }
		}
	}
} catch(ErrorException $e) {
    error_log("1220 - ErrorException occured: " . $e->getMessage());
	$msg = "{\"success\":false,\"error\":\"1220\"}";
    if ($app){ consoleExit($msg); }
    else { exit($msg); }
}
