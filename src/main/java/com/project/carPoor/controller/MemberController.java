package com.project.carPoor.controller;

import com.project.carPoor.service.EmailService;
import com.project.carPoor.domain.EmailMessage;
import com.project.carPoor.domain.Member;
import com.project.carPoor.service.MemberService;
import com.project.carPoor.validator.CheckEmailValidator;
import com.project.carPoor.validator.CheckLoginIdValidateor;
import com.project.carPoor.validator.CheckPasswordValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final CheckEmailValidator checkEmailValidator;
    private final CheckLoginIdValidateor checkLoginIdValidateor;
    private final CheckPasswordValidator checkPasswordValidator;
    private final EmailService emailService;

    @InitBinder // Controller 내 Binding, 검증 절차 설정
    public void validatorBinder(WebDataBinder binder) {

        binder.addValidators(checkEmailValidator);
        binder.addValidators(checkLoginIdValidateor);
        binder.addValidators(checkPasswordValidator);
        
    }

    // signIn 과 signUp 의 페이지가 같아보이지만 다름. url 을 잘 체크할 것
    // signIn 에서만 로그인이 가능. url 이 signUp(create) 이면 눈에 보이는 페이지가 같아도 실행 불가능
    // signUp 에서만 회원가입이 가능. ( member/create ) 각 기능 점검시 url 이동에 주의할 것.

    @GetMapping("/sign")
    public String showSign(SignupForm signupForm, Model model) {

        model.addAttribute("SignupForm", signupForm);

        return "/member/signIn";
    }

    @PostMapping("/create")
    public String doSignUp(@Valid SignupForm signupForm, Errors errors, Model model) {

        if(errors.hasErrors()) {   // 유효성 검사

            // signup 실패 시 value 값 유지
            model.addAttribute("SignupForm", signupForm);

            // 유효성 검사 통과 못한 필드와 그 message 를  핸들링
            Map<String, String> validatorResult = memberService.validateHandling(errors); // service 에 handling 요청, 결과 받기

            for (String key : validatorResult.keySet()) { // 받아온 handling 결과 model 로 전달
                model.addAttribute(key, validatorResult.get(key));
            }

            // page 재 return
            return "/member/signUp";

        }

        // 유효성 검사 통과 시 회원 저장

        Member member = new Member();

        member.setLoginId(signupForm.getLoginId());
        member.setPassword(passwordEncoder.encode(signupForm.getPassword1()));
        member.setName(signupForm.getName());
        member.setPhoneNumber(signupForm.getPhoneNumber());
        member.setEmail(signupForm.getEmail());

        EmailMessage emailMessage = EmailMessage.builder() // 이메일 전송을 위한 data 준비
                .to(signupForm.getEmail())
                .subject("[carPoor] 회원가입 이메일 인증")
                .build();

        member.setAuthKey(emailService.sendMail(emailMessage, "email")); // authkey 생성, 인증 이메일 발송요청(해당 html 파일 전달), authkey 저장
        member.setJoinStatus(false); // 인증 전

        this.memberService.createMember(member);

        model.addAttribute("Message", "이메일 인증을 완료하면 회원가입이 완료됩니다.");

        return "/member/signupAlert";
    }

    @GetMapping("/emailConfirm")
    @Transactional
    public String doEmailConfirm(String email, String code, Model model) { // 사용자 이메일 인증 mapping

        Member member = this.memberService.getMemberByEmail(email);

        if(code.equals(member.getAuthKey())) {

            member.setJoinStatus(true); // 계정 상태 전환
        }

        model.addAttribute("Message", "[carPoor] 회원가입이 완료되었습니다!");

        return "/member/emailAlert";
    }



}
