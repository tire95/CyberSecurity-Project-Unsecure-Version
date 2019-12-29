# CyberSecurity Project - Unsecure Version
 
The goal was to create a web application with at least 5 security flaws from [OWASP Top 10 list](https://www.owasp.org/images/7/72/OWASP_Top_10-2017_%28en%29.pdf.pdf). The application was done with Java and Spring Framework.

A version with all the security fixes implemented can be found [here](https://github.com/tire95/CyberSecurity-Project--Secure-Version).

**FLAW 1:**

OWASP A3: Sensitive Data Exposure - Passwords are saved as plain text.

Solution: The problem can be fixed by encrypting the passwords. The easiest way is with Spring's `BCryptPasswordEncoder`; add the following lines to `SecurityConfiguration`

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
	
Now, when a new password is set, it must be encrypted. Add the following to `WebsiteController`

    @Autowired
    private PasswordEncoder passwordEncoder;

and edit the `register` method so that the password is encoded; i.e. from

	account.setPassword(password);
	
to

	account.setPassword(passwordEncoder.encode(password));


**FLAW 2:**

OWASP A2: Broken Authentication - Session ID is not invalidated after a period of inactivity.

Solution: The easiest solution is to add `server.session.timeout=300` to `application.properties` - this makes the session expire after 5 minutes of inactivity - and `<meta http-equiv="refresh" content="300"></meta>` to the head of `notebook.html` - this makes `notebook.html` autorefresh after 5 minutes of inactivity, and since the session has expired at this point, the browser is directed back to the login page. Inactivity, in this case, means that the user has not committed any changes to the application, i.e. logging out, creating a new note, or deleting an existing note; in other words, simply writing something into a field will not restart the timer.

We can also add a timer to `notebook.html` that tells when the session will timeout; add `<b>Session will timeout in <span id='timer'></span></b>` to the pages's body, and add the following to the page's head:

	<script>
		/*<![CDATA[*/
		window.onload = function() {
			document.getElementById('timer').innerHTML =
			5 + ":" + 00;
			startTimer();
		};

		function startTimer() {
			var presentTime = document.getElementById('timer').innerHTML;
			var timeArray = presentTime.split(/[:]+/);
			var m = timeArray[0];
			var s = checkSecond((timeArray[1] - 1));
			if(s >= 59) {m = m-1;}

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


**FLAW 3:**

OWASP A10: Insufficient Logging & Monitoring: The application doesn't create any logs.

Solution: Add the following lines to `application.properties`

	server.tomcat.accesslog.enabled=true
	server.tomcat.accesslog.suffix=.log
	server.tomcat.accesslog.prefix=access_log
	server.tomcat.basedir=tomcat
	server.tomcat.accesslog.directory=logs
	
Now the application creates logs into the directory `/tomcat/logs`

The logs look like

	0:0:0:0:0:0:0:1 - - [29/Dec/2019:15:52:24 +0200] "GET /login HTTP/1.1" 200 1045
	
and they have the following format:

	%ip %i %u %t %r %s %b
	
Which we can interpret as:

- %ip – the client IP which has sent the request, *0:0:0:0:0:0:0:1* in this case
- %i – the identity of the user
- %u – the user name determined by HTTP authentication
- %t – the time the request was received
- %r – the request line from the client, *GET /login HTTP/1.1* in this case
- %s – the status code sent from the server to the client, *200* in this case
- %b – the size of the response to the client, *1045* in this case

Since this request didn't have an authenticated user, %i and %u printed dashes.


**FLAW 4:**

OWASP A2: Broken Authentication - The application permits short and well-known passwords.

Solution: First of all, edit the password input in the `register.html` from

    <input placeholder="Password" type="password" name="password"/>

to

	<input placeholder="Password" type="password" name="password" pattern=".{8,}" title="8 characters minimum"/>
	
This makes the minimum password length 8 characters.

The next step is prohibiting well-known passwords from being used. Copy any password list from [this repository](https://github.com/danielmiessler/SecLists/tree/master/Passwords) and save it as `passwords.txt` to `/src/main/resources`

To check whether a given password is well-known, we could save the passwords in `passwords.txt` to an ArrayList and just check with `arrayList.contains(password)`. However, if the list of passwords is large, the memory of the JVM might run out. This is why saving the passwords to the database might be a good idea, even though retrieving information from a database can be slower.

Create the following class called "Password" to folder `sec.project.domain`

	@Entity
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	public class Password extends AbstractPersistable<Long> {
		
		private String password;
		
	}

Create the following interface called "PasswordRepository" to folder `sec.project.repository`

	public interface PasswordRepository extends JpaRepository<Password, Long> {
		
		Password findByPassword(String username);
		
	}
	
Add the following code to `CustomUserDetailsService`

    @Autowired
    PasswordRepository passwordRepository;
    
    @PostConstruct
    public void init() {
        try (InputStream input = new FileInputStream("./src/main/resources/passwords.txt")) {
            Scanner reader = new Scanner(input);
            while(reader.hasNextLine()) {
                passwordRepository.save(new Password(reader.nextLine().toLowerCase()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
	
Edit the `register` method in `WebsiteController` from

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@RequestParam String username, @RequestParam String password, Model model) {
        if (accountRepository.findByUsername(username) != null) {
            String error = "Username already exists";
            model.addAttribute("errorMsg", error);
            return "register";
        }
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        accountRepository.save(account);
        return "redirect:/login";
    }
	
to

	@RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@RequestParam String username, @RequestParam String password, Model model) {
        if (accountRepository.findByUsername(username) != null || passwordRepository.findByPassword(password.toLowerCase()) != null) {
            String error = "Invalid username and/or password";
            model.addAttribute("errorMsg", error);
            return "register";
        }
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        accountRepository.save(account);
        return "redirect:/login";
    }

Now the application won't allow common passwords, and the checking is case insensitive.

This solution, of course, is not perfect; for example, not allowing passwords that are concatenations of two or more common passwords is a good additional rule.


**FLAW 5:**

OWASP A2: Broken Authentication - There is no control for concurrent sessions for a single user; a user can be signed in on two different computers, or on two different browsers on the same computer.

Solution: Add `http.sessionManagement().maximumSessions(1);` to `configure` method in `SecurityConfiguration`; this way, when an already authenticated user signs in on another computer/browser, the previous session is invalidated.


**FLAW 6:**

OWASP A7: Cross-Site Scripting (XSS) - Stored XSS attack is possible by injecting JavaScript to either note's title or content.

Solution: Replace the two `th:utext`s in `notebook.html` with `th:text`