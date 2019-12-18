# CyberSecurity Project - Unsecure Version
 
The goal was to create a web application with at least 5 security flaws from [OWASP Top 10 list](https://www.owasp.org/images/7/72/OWASP_Top_10-2017_%28en%29.pdf.pdf). The application was done with Java and Spring Framework.

FLAW 1: 
OWASP A3: Sensitive Data Exposure - Passwords are saved as plain text
Solution: This can be fixed by encrypting the passwords; the easiest way is with Spring's `BCryptPasswordEncoder`. Add the following lines to `SecurityConfiguration` and add the necessary imports.

	@Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
Now, every time a new password is set, it must be encrypted; for example, in `CustomUserDetailsService`'s initialization, user Khorne's password must be set with `account.setPassword(passwordEncoder.encode("Milk for Khorne flakes!"));`.
	
FLAW 2:
OWASP A2: Broken Authentication - There is no authentication after a period of inactivity
Solution: Add `server.session.timeout=15` to `application.properties` and `<meta http-equiv="refresh" content="900"></meta>` to `notebook.html`'s head. This makes the session timeout after 15 min of inactivity, and `notebook.html` autorefreshes, which directs the browser back to the login page.

We can also add a timer to the page that tells when the session will timeout; add `<b>Session will timeout in <span id='timer'></span></b>` to `notebook.html`'s body, and add the following to the page's head:

    <script>
        /*<![CDATA[*/
        window.onload = function() {
            document.getElementById('timer').innerHTML =
            14 + ":" + 59;
            startTimer();
        };

        function startTimer() {
            var presentTime = document.getElementById('timer').innerHTML;
            var timeArray = presentTime.split(/[:]+/);
            var m = timeArray[0];
            var s = checkSecond((timeArray[1] - 1));
            if(s===59){m=m-1;}

            document.getElementById('timer').innerHTML = m + ":" + s;
            setTimeout(startTimer, 1000);
        }

        function checkSecond(sec) {
            if (sec < 10 && sec >= 0) {sec = "0" + sec;}
            if (sec < 0) {sec = "59";}
            return sec;
        }
        /*]]>*/
    </script>
	
FLAW 3:
A7: Cross-Site Scripting (XSS) - Stored XSS attack is possible
Solution: Replace the two `th:utext`s in `notebook.html` with `th:text`.