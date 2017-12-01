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
            require_once("makeAccount"); // This will do the rest for us.
		} else {
            error_log("1219 - OpenID failed to validate. Reason unknown.");
			consoleExit("{\"success\":false,\"error\":\"1219\"}");
		}
	}
} catch(ErrorException $e) {
    error_log("1220 - ErrorException occured: " . $e->getMessage());
	consoleExit("{\"success\":false,\"error\":\"1220\"}");
}
