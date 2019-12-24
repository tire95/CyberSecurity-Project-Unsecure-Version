# CyberSecurity Project - Unsecure Version
 
The goal was to create a web application with at least 5 security flaws from [OWASP Top 10 list](https://www.owasp.org/images/7/72/OWASP_Top_10-2017_%28en%29.pdf.pdf). The application was done with Java and Spring Framework.


FLAW 1:

OWASP A3: Sensitive Data Exposure - Passwords are saved as plain text.

Solution: This can be fixed by encrypting the passwords. The easiest way is with Spring's `BCryptPasswordEncoder`; add the following lines to `SecurityConfiguration` and add the necessary imports.

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
	
Now, every time a new password is set, it must be encrypted; for example, in `CustomUserDetailsService`'s initialization, user Khorne's password must be set with `account.setPassword(passwordEncoder.encode("MilkforKhorneflakes!"));`


FLAW 2:

OWASP A2: Broken Authentication - There is no authentication after a period of inactivity.

Solution: Add `server.session.timeout=15` to `application.properties` and `<meta http-equiv="refresh" content="900"></meta>` to `notebook.html`'s head. This makes the session timeout after 15 min of inactivity, and `notebook.html` autorefreshes, directing the browser back to the login page.

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

OWASP A7: Cross-Site Scripting (XSS) - Stored XSS attack is possible by injecting JavaScript to either note's title or content.

Solution: Replace the two `th:utext`s in `notebook.html` with `th:text`


FLAW 4:

OWASP A10: Insufficient Logging & Monitoring: The application doesn't create any logs.

Solution: Add the following lines to `application.properties`

	server.tomcat.accesslog.enabled=true
	server.tomcat.accesslog.suffix=.log
	server.tomcat.accesslog.prefix=access_log
	server.tomcat.basedir=tomcat
	server.tomcat.accesslog.directory=logs
	
Now the application creates logs into the category `/tomcat/logs`


FLAW 5:

OWASP A2: Broken Authentication - The application permits short and well-known passwords.

Solution: First of all, edit the password input in the `register.html` to `<input placeholder="Password" type="password" name="password" pattern=".{8,}" title="8 characters minimum"/>`. This makes the minimum password length 8 characters.

The next step is prohibiting well-known passwords from being used. Copy any password list from [this repository](https://github.com/danielmiessler/SecLists/tree/master/Passwords) and save it as `passwords.txt` to `/src/main/resources`.

Create the following class to folder `domain`

	@Entity
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	public class Password extends AbstractPersistable<Long> {
		
		private String password;
		
	}

Create the following interface to folder `repository`

	public interface PasswordRepository extends JpaRepository<Password, Long> {
		
		Password findByPassword(String username);
		
	}
	
Add the following snippet of code to the `init` method in `CustomUserDetailsService`

    try (InputStream input = new FileInputStream("./src/main/resources/passwords.txt")) {
        Scanner reader = new Scanner(input);
        while(reader.hasNextLine()) {
            passwordRepository.save(new Password(reader.nextLine().toLowerCase()));
        }
    } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
	
Add the following snippet of code to the `register` method in `WebsiteController` (obviously before the account is saved to the database)

    if (passwordRepository.findByPassword(password.toLowerCase()) != null) {
        String error = "The password you tried is too common, try another one";
        model.addAttribute("errorMsg", error);
        return "register";
    }
	
Finally, add `<p style="color:red" th:text='${errorMsg}'></p>` to `register.html`
