package com.javahash.spring.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.javahash.spring.dao.BookDAO;
import com.javahash.spring.model.Book;

@Controller
public class HelloWorldController { 
	
	@Autowired
	private BookDAO bookDao;
	
	@RequestMapping("/find")
	public String initFindForm(Model model){
		model.addAttribute("book",new Book());
		return "findbooks";
	}

    @RequestMapping("/books")
    public String hello(Book book, Model model, BindingResult result) {
        if (book.getTitle() == null) {
        	book.setTitle(""); // empty string signifies broadest possible search
        }
        
        // find books by title
        List<Book> results = bookDao.findBookByTitle(book.getTitle());
        
        if (results.size() > 0)
        {
	        model.addAttribute("selections", results);
	        return "booklist";
        }
        else 
        {
        	result.rejectValue("title", "not found", "This book title was not found");
        	return "findbooks";
        }
    }
    
    @RequestMapping(value= "/edit", method = RequestMethod.GET)
    public String editBook(Model model,  HttpServletRequest request)
    {
    	int bookId = Integer.parseInt(request.getParameter("id"));
    	Book book = bookDao.get(bookId);
    	model.addAttribute("book",book);
    	return "updateBookForm";
    }
    
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String processUpdateBookForm(Book book, BindingResult result, SessionStatus status) {
        if (result.hasErrors()) {
            return "updateOwnerForm";
        } else {
            bookDao.saveOrUpdate(book);
            status.setComplete();
            return "redirect:/books";
        }
    }
    
    @RequestMapping(value="/deleteQuestion")
    public String deleteQuestion(Model model, HttpServletRequest request)
    {
    	int bookId = Integer.parseInt(request.getParameter("id"));
    	Book book = bookDao.get(bookId);
    	model.addAttribute("book",book);
    	return "deleteQuestion";
    }
    
    @RequestMapping(value="/delete")
    public String deleteBook(HttpServletRequest request)
    {
    	int bookId = Integer.parseInt(request.getParameter("id"));
    	Book book = bookDao.get(bookId);
    	bookDao.deleteBook(book);
    	return "redirect:/books";
    }
    
    @RequestMapping(value="/new", method=RequestMethod.GET)
    public String createBook(Model model)
    {
    	Book book = new Book();
    	model.addAttribute("book",book);
    	return "createBookForm";
    }
    
    @RequestMapping(value="/new", method=RequestMethod.POST)
    public String processCreateBook(Book book, SessionStatus status)
    {	
    	//book.setId(1);
    	bookDao.saveOrUpdate(book);
    	status.setComplete();
    	return "redirect:/books";
    }
    
    @RequestMapping("/greet")
    public String greet(Model model) {
        return "greeting";
    }
    //**************************************************************************
    //
    //	Spring Security mappings
    //
    //**************************************************************************
    
    @RequestMapping(value = { "/", "/welcome**" }, method = RequestMethod.GET)
	public ModelAndView welcomePage() {

		ModelAndView model = new ModelAndView();
		model.addObject("title", "Spring Security Hello World");
		model.addObject("message", "This is welcome page!");
		model.setViewName("hello");
		return model;

	}

	@RequestMapping(value = "/admin**", method = RequestMethod.GET)
	public ModelAndView adminPage() {

		ModelAndView model = new ModelAndView();
		model.addObject("title", "Spring Security Hello World");
		model.addObject("message", "This is protected page - Admin Page!");
		model.setViewName("admin");

		return model;

	}
	
	/**
	 * This update page is for user login with password only.
	 * If user is login via remember me cookie, send login to ask for password again.
	 * To avoid stolen remember me cookie to update info
	 */
	@RequestMapping(value = "/admin/update**", method = RequestMethod.GET)
	public ModelAndView updatePage(HttpServletRequest request) {

		ModelAndView model = new ModelAndView();

		if (isRememberMeAuthenticated()) {
			//send login for update
			setRememberMeTargetUrlToSession(request);
			model.addObject("loginUpdate", true);
			model.setViewName("/login");

		} else {
			model.setViewName("update");
		}

		return model;

	}

	//Spring Security see this :
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView login(
		@RequestParam(value = "error", required = false) String error,
		@RequestParam(value = "logout", required = false) String logout,
						HttpServletRequest request) {

		ModelAndView model = new ModelAndView();
		if (error != null) {
			model.addObject("error", getErrorMessage(request, "SPRING_SECURITY_LAST_EXCEPTION"));
			
			//login form for update page
            //if login error, get the targetUrl from session again.
			String targetUrl = getRememberMeTargetUrlFromSession(request);
			System.out.println(targetUrl);
			if(StringUtils.hasText(targetUrl)){
				model.addObject("targetUrl", targetUrl);
				model.addObject("loginUpdate", true);
			}
		}

		if (logout != null) {
			model.addObject("msg", "You've been logged out successfully.");
		}
		model.setViewName("login");

		return model;

	}
	
	//for 403 access denied page
	@RequestMapping(value = "/403", method = RequestMethod.GET)
	public ModelAndView accesssDenied() {

	  ModelAndView model = new ModelAndView();

	  //check if user is login
	  Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	  if (!(auth instanceof AnonymousAuthenticationToken)) {
		UserDetails userDetail = (UserDetails) auth.getPrincipal();
		model.addObject("username", userDetail.getUsername());
	  }

	  model.setViewName("403");
	  return model;

	}
	
	//**** METHODS ***
	
	//customize the error message
	private String getErrorMessage(HttpServletRequest request, String key){

		Exception exception =
                   (Exception) request.getSession().getAttribute(key);

		String error = "";
		if (exception instanceof BadCredentialsException) {
			error = "Invalid username and password!";
		}else if(exception instanceof LockedException) {
			error = exception.getMessage();
		}else{
			error = "Invalid username and password!";
		}

		return error;
	}
	
	/**
	 * Check if user is login by remember me cookie, refer
	 * org.springframework.security.authentication.AuthenticationTrustResolverImpl
	 */
	private boolean isRememberMeAuthenticated() {

		Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}

		return RememberMeAuthenticationToken.class.isAssignableFrom(authentication.getClass());
	}
	
	/**
	 * save targetURL in session
	 */
	private void setRememberMeTargetUrlToSession(HttpServletRequest request){
		HttpSession session = request.getSession(false);
		if(session!=null){
			session.setAttribute("targetUrl", "/admin/update");
		}
	}
	
	/**
	 * get targetURL from session
	 */
	private String getRememberMeTargetUrlFromSession(HttpServletRequest request){
		String targetUrl = "";
		HttpSession session = request.getSession(false);
		if(session!=null){
			targetUrl = session.getAttribute("targetUrl")==null?""
                             :session.getAttribute("targetUrl").toString();
		}
		return targetUrl;
	}

}