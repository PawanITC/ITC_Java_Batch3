package com.itc.funkart.assignments.priyanthan;

interface PasswordValidator{
    boolean isPasswordValid(String password)
}

class BasicPasswordValidator implements PasswordValidator{

    public isPasswordValid(String password){
        return password.length() > 8 && password.length() <= 24;
    }

}




public class SolidPriyanthan {
    //Single responsibility class with only one responsibility i.e
    //validating email and passwor
    //
     //Open closed principles - This class shows you can pass an implementation without modifying the class.
    private PasswordValidator passwordValidator;
    public SolidPriyanthan(PasswordValidator passwordValidator){
        this.passwordValidator = passwordValidator;
    }

    public boolean validatePassword(String password){
        return this.passwordValidator.isPasswordValid(password);
    }

    public boolean isEmailValid(String email){
        return email.contains("@");
    }




}
