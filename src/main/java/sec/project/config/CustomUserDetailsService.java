package sec.project.config;

import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sec.project.domain.Account;
import sec.project.domain.Note;
import sec.project.repository.AccountRepository;
import sec.project.repository.NoteRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {


    @Autowired
    AccountRepository accountRepository;
    
    @Autowired
    NoteRepository noteRepository;
    
    @PostConstruct
    public void init() {
        Account account = new Account();
        account.setUsername("Khorne");
        account.setPassword("Milk for Khorne flakes!");
        Account account2 = new Account();
        account2.setUsername("Leomund");
        account2.setPassword("Tiny Hut");
        Note note = new Note();
        note.setTitle("Shopping list");
        note.setContent("Blood for me, skulls for the throne, milk for the flakes");
        note.setAccount(account);
        List<Note> notes1 = account.getNotes();
        notes1.add(note);
        account.setNotes(notes1);
        Note note2 = new Note();
        note2.setTitle("Shopping list");
        note2.setContent("A small crystal bead");
        note2.setAccount(account2);
        List<Note> notes2 = account2.getNotes();
        notes2.add(note2);
        account2.setNotes(notes2);
        accountRepository.save(account);
        accountRepository.save(account2);
        noteRepository.save(note);
        noteRepository.save(note2);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("No such user: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                account.getUsername(),
                account.getPassword(),
                true,
                true,
                true,
                true,
                Arrays.asList(new SimpleGrantedAuthority("USER")));
    }
}
