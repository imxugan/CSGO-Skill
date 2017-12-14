## Errors Explained
Every script has it's own unique error codes, which are built using the generic
errors. Every script has it's own unique prefix to help locate exactly where
the error happened. Because the same generic error can be thrown in different
places in the same script, every script will count up after each use of the
same error. Here is an example of what an error code could look like: `10123`

## Counting Up
Just count up from 1 for multiple uses of the same general error.

## Script Prefixes

|    Script Name    |       Location       | Prefix |
| :---              | :---                 | :---:  |
| `version.php`     | `api.csgo-skill.com` |  `10`  |
| `getGuides.php`   | `api.csgo-skill.com` |  `11`  |
| `login.php`       | `api.csgo-skill.com` |  `12`  |
| `makeAccount.php` | `web/includes/`      |  `13`  |
| `addAccount.php`  | `api.csgo-skill.com` |  `14`  |
| `verifyEmail.php` | `csgo-skill.com/`    |  `15`  |
| `atom.php`        | `csgo-skill.com/`    |  `16`  |

## Web API errors ()

|    Error Constant     | Code  |
| :---                  | :---: |
| `CONNECTION_ERROR`    | `12`  |
| `EMPTY_RESULT`        | `13`  |
| `MISSING_FIELD`       | `14`  |
| `JSON_ERROR`          | `15`  |
| `UNSUPPORTED_VERSION` | `16`  |
| `FROM_THE_FUTURE`     | `17`  |
| `CANCELED_AUTH`       | `18`  |
| `NO_LOGIN`            | `19`  |
| `MYSTERY_ERROR`       | `20`  |
| `PROFILE_NOT_PUBLIC`  | `21`  |
| `BAD_PROFILE`         | `22`  |
| `BAD_FIELD`           | `23`  |
| `ALREADY_EXISTS`      | `24`  |
| `BAD_FORM`            | `25`  |
| `QUERY_ERROR`         | `26`  |
| `NO_SUCH_PROFILE`     | `27`  |
| `TOO_BIG`             | `28`  |
