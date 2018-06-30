define({ "api": [
  {
    "type": "get",
    "url": "/login",
    "title": "GET",
    "version": "0.1.0",
    "name": "GetLogin",
    "group": "Login",
    "description": "<p>Used to initially log users in. Simply redirects new requests to the Steam Open ID login page, and takes Open ID requests to finish the login process. NOTE: Responses are sent through the javascript console messages, it can also be found in a hidden span element named &quot;out&quot;.</p>",
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Object",
            "optional": false,
            "field": "profile",
            "description": "<p>Full profile of the user</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"profile\": {..}\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"OpenIdError3\"\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "AuthCanceled",
            "description": "<p>OpenID specific error, authentication was canceled by the user</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadMongos",
            "description": "<p>Mongo DB internal problems</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "OpenIdErrorX",
            "description": "<p>Generic OpenID error, where X is a unique error number</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "Login"
  },
  {
    "type": "post",
    "url": "/login",
    "title": "POST",
    "version": "0.1.0",
    "name": "PostLogin",
    "group": "Login",
    "description": "<p>Let's the user refresh their login token by posting the current one, probably not optimal security but honestly this service isn't that crazy right now.</p>",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "steamId",
            "description": "<p>User's Steam ID</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "token",
            "description": "<p>User's token hash</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "Object",
            "optional": false,
            "field": "profile",
            "description": "<p>Full profile of the user</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"profile\": {..}\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"LoginRequired\"\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "BadId",
            "description": "<p>Invalid Steam ID</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadMongos",
            "description": "<p>Mongo DB internal problems</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadRequest",
            "description": "<p>Incorrect request, missing fields, bad data</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadToken",
            "description": "<p>Invalid Token object ({time: 123, value: &quot;abcdef0123456789&quot;})</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "LoginRequired",
            "description": "<p>Indicates that the operation requires a fresh login, such as the token expiring</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NotFound",
            "description": "<p>Generic error for a profile not found</p> <p>This can also be returned when a profile exists, but the authentication was incorrect, as to prevent exposing the profile.</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "Login"
  },
  {
    "type": "post",
    "url": "/signup",
    "title": "POST",
    "version": "0.1.0",
    "name": "PostSignup",
    "group": "Signup",
    "description": "<p>Allows existing players to become users by connecting an email to their account and selecting a unique username. The user can also pick a new persona name to be displayed instead of the Steam persona, if they want to.</p>",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "size": "4..50",
            "optional": false,
            "field": "email",
            "description": "<p>Email address to connect</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "size": "3..35",
            "optional": false,
            "field": "persona",
            "description": "<p>Persona name, if the same as current, then the persona override is not activated</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "size": "3..35",
            "optional": false,
            "field": "username",
            "description": "<p>A unique, URL friendly username. Only certain symbols are disallowed, therefore usernames like 可愛い or толстый are acceptable</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "steamId",
            "description": "<p>User's Steam ID, to connect the email and username with</p>"
          },
          {
            "group": "Parameter",
            "type": "string",
            "optional": false,
            "field": "token",
            "description": "<p>User's token hash</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "object",
            "optional": false,
            "field": "profile",
            "description": "<p>Updated profile with new email, username, and persona</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"profile\": {..}\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"NameEmailTaken\"\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "AlreadyExists",
            "description": "<p>Specifies that, during signup, the account is already registered</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadEmail",
            "description": "<p>Invalid email address</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadId",
            "description": "<p>Invalid Steam ID</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadMongos",
            "description": "<p>Mongo DB internal problems</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadPersona",
            "description": "<p>Invalid fursona name</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadRequest",
            "description": "<p>Incorrect request, missing fields, bad data</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadToken",
            "description": "<p>Invalid Token object ({time: 123, value: &quot;abcdef0123456789&quot;})</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadUsername",
            "description": "<p>Invalid username</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "EmailTaken",
            "description": "<p>The provided email is already taken</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "LoginRequired",
            "description": "<p>Indicates that the operation requires a fresh login, such as the token expiring</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NameTaken",
            "description": "<p>The provided username is already taken</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NameEmailTaken",
            "description": "<p>A special case where the username and email are taken, we tell the user they may already have an account, so that they may use that one instad of attempting to register another</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NotFound",
            "description": "<p>Generic error for a profile not found</p> <p>This can also be returned when a profile exists, but the authentication was incorrect, as to prevent exposing the profile.</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "Signup"
  },
  {
    "type": "get",
    "url": "/stats/:id",
    "title": "User Stats",
    "version": "0.1.0",
    "name": "GetStats",
    "group": "Static",
    "description": "<p>Returns the given user's public stats. The special user &quot;mega&quot; returns site-wide stats in a unique format.</p>",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "id",
            "description": "<p>Username</p>"
          }
        ]
      }
    },
    "success": {
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"user\": {..},\n  \"stats\": {\n    \"current\": {..},\n    \"grand\": {..},\n    \"history\": [\n      {..},\n      {..},\n      ..\n    ]\n  }\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"BadMongos\"\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "BadMongos",
            "description": "<p>Mongo DB internal problems</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NotFound",
            "description": "<p>Generic error for a profile not found</p> <p>This can also be returned when a profile exists, but the authentication was incorrect, as to prevent exposing the profile.</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "Static"
  },
  {
    "type": "get",
    "url": "/version",
    "title": "API Version",
    "version": "0.1.0",
    "name": "GetVersion",
    "group": "Static",
    "description": "<p>Returns the current project version, syncronized between the website and apps. This is represented by a list of MAJOR, MINOR, and PATCH versions.</p>",
    "success": {
      "examples": [
        {
          "title": "Success:",
          "content": "[0, 1, 0]",
          "type": "json"
        }
      ]
    },
    "filename": "lib/api.js",
    "groupTitle": "Static"
  },
  {
    "type": "get",
    "url": "/user/:id",
    "title": "GET",
    "version": "0.1.0",
    "name": "GetUser",
    "group": "User",
    "description": "<p>Retrieves public information about a user's profile</p>",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "id",
            "description": "<p>User's Steam ID or Username</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "object",
            "optional": false,
            "field": "profile",
            "description": "<p>Current profile information</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"profile\": {..}\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"NotFound\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "BadMongos",
            "description": "<p>Mongo DB internal problems</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NotFound",
            "description": "<p>Generic error for a profile not found</p> <p>This can also be returned when a profile exists, but the authentication was incorrect, as to prevent exposing the profile.</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "User"
  },
  {
    "type": "post",
    "url": "/user/:id",
    "title": "POST",
    "version": "0.1.0",
    "name": "PostUser",
    "group": "User",
    "description": "<p>Updates the user's account information,</p>",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "id",
            "description": "<p>Username</p>"
          }
        ],
        "Post": [
          {
            "group": "Post",
            "type": "String",
            "optional": false,
            "field": "steamId",
            "description": "<p>User's Steam ID</p>"
          },
          {
            "group": "Post",
            "type": "String",
            "optional": false,
            "field": "token",
            "description": "<p>User's token hash</p>"
          },
          {
            "group": "Post",
            "type": "Object",
            "optional": false,
            "field": "ops",
            "description": "<p>Profile operations, for changing the email or persona name</p>"
          }
        ]
      }
    },
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "type": "object",
            "optional": false,
            "field": "profile",
            "description": "<p>Current profile information</p>"
          }
        ]
      },
      "examples": [
        {
          "title": "Success:",
          "content": "{\n  \"success\": true,\n  \"profile\": {..}\n}",
          "type": "json"
        }
      ]
    },
    "error": {
      "examples": [
        {
          "title": "Error:",
          "content": "{\n  \"success\": false,\n  \"reason\": \"LoginRequired\"\n}",
          "type": "json"
        }
      ],
      "fields": {
        "Error": [
          {
            "group": "Error",
            "optional": false,
            "field": "BadEmail",
            "description": "<p>Invalid email address</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadId",
            "description": "<p>Invalid Steam ID</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadPersona",
            "description": "<p>Invalid fursona name</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadRequest",
            "description": "<p>Incorrect request, missing fields, bad data</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadToken",
            "description": "<p>Invalid Token object ({time: 123, value: &quot;abcdef0123456789&quot;})</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "BadUsername",
            "description": "<p>Invalid username</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "LoginRequired",
            "description": "<p>Indicates that the operation requires a fresh login, such as the token expiring</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "MultipleEmailChanges",
            "description": "<p>When a user attempts to change their email to frequently, or before a previous email is verified</p>"
          },
          {
            "group": "Error",
            "optional": false,
            "field": "NotFound",
            "description": "<p>Generic error for a profile not found</p> <p>This can also be returned when a profile exists, but the authentication was incorrect, as to prevent exposing the profile.</p>"
          }
        ]
      }
    },
    "filename": "lib/api.js",
    "groupTitle": "User"
  }
] });
