package com.example.remotehomeelectricalcontrolsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.remotehomeelectricalcontrolsystem.Model.SharedUser;
import com.example.remotehomeelectricalcontrolsystem.Model.UserHouse;
import com.example.remotehomeelectricalcontrolsystem.Utils.EncryptionUtils;
import com.example.remotehomeelectricalcontrolsystem.Utils.Format;
import com.example.remotehomeelectricalcontrolsystem.Utils.InputValidator;
import com.example.remotehomeelectricalcontrolsystem.Model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class SignupActivity extends AppCompatActivity {
  private FirebaseDatabase db;
  private DatabaseReference usersRef, housesRef, usersHousesRef;
  TextInputEditText edtName, edtEmail, edtTelephone, edtHouseKey, edtPassword;
  TextInputLayout layoutName, layoutEmail, layoutTelephone, layoutHouseKey, layoutPassword;
  TextView txtLogin;
  Button btnSignUp;
//  UUID uuid;
  String role, houseId;
  boolean[] isValidForm = {false, false, false, false, false};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signup);

    init();

    edtName.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String name = edtName.getText().toString();
        String fullNameError = InputValidator.isValidFullName(name);
        if (fullNameError != null) {
          edtName.setError(fullNameError);
          isValidForm[0] = false;
        } else {
          edtName.setError(null);
          isValidForm[0] = true;
        }
      }
    });

    edtEmail.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String email = edtEmail.getText().toString();
        String emailError = InputValidator.isValidEmail(email);
        if (emailError != null) {
          edtEmail.setError(emailError);
          isValidForm[1] = false;
        } else {
          edtEmail.setError(null);
          isValidForm[1] = true;
        }
      }
    });

    edtTelephone.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String tel = edtTelephone.getText().toString();
        String telError = InputValidator.isValidPhoneNumber(tel);
        if (telError != null) {
          edtTelephone.setError(telError);
          isValidForm[2] = false;
        } else {
          edtTelephone.setError(null);
          isValidForm[2] = true;
        }
      }
    });

    edtHouseKey.setOnFocusChangeListener((view, b) -> {
      if (!b) {
        String houseKey = edtHouseKey.getText().toString();
        checkHouseKeyExist(houseKey);
      }
    });

    edtPassword.setOnFocusChangeListener((view, hasFocus) -> {
      if (!hasFocus) {
        String password = edtPassword.getText().toString();
        String passwordError = InputValidator.isValidPassword(password);
        if (passwordError != null) {
          edtPassword.setError(passwordError);
          isValidForm[4] = false;
        } else {
          isValidForm[4] = true;
        }
      }
    });

    btnSignUp.setOnClickListener(v -> {
      String email = edtEmail.getText().toString();
      String password = edtPassword.getText().toString();
      String fullName = edtName.getText().toString();
      String houseKey = edtHouseKey.getText().toString();
      String telephone = edtTelephone.getText().toString();

      clearFocusFromInputField();

      if (checkAllFields(fullName, email, telephone, houseKey, password)) {
        checkRegisteredAccount(email, houseKey, fullName, telephone, password);
        // Do not continue with the code here
      } else {
        Toast.makeText(SignupActivity.this, "Please check your registration information again", Toast.LENGTH_LONG).show();
      }
    });

    txtLogin.setOnClickListener(v -> {
      Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
      startActivity(intent);
    });
  }

  public void init() {
    edtName = findViewById(R.id.edtName);
    edtEmail = findViewById(R.id.edtEmail);
    edtPassword = findViewById(R.id.edtPassword);
    edtHouseKey = findViewById(R.id.edtHouseKey);
    layoutHouseKey = findViewById(R.id.layoutHouseKey);
    edtTelephone = findViewById(R.id.edtTelephone);
    txtLogin = findViewById(R.id.txtLogin);
    btnSignUp = findViewById(R.id.btnSignUp);
    db = FirebaseDatabase.getInstance();
    usersRef = db.getReference("users");
    housesRef = db.getReference("test1");
    usersHousesRef = db.getReference("usersHouses");
  }

  public boolean checkAllFields(String name, String email, String telephone, String houseKey, String password) {
    boolean[] isValidForm = InputValidator.areAllFieldsNotEmpty(name, telephone, email, houseKey, password);
    boolean allFieldsValid = true;
    for (int i = 0; i < isValidForm.length; i++) {
      if (!isValidForm[i]) {
        allFieldsValid = false;
        if (i == 0) edtName.setError("This field is required");
        else if (i == 1) edtEmail.setError("This field is required");
        else if (i == 2) edtTelephone.setError("This field is required");
        else if (i == 3) edtHouseKey.setError("This field is required");
        else edtPassword.setError("This field is required");
      }
    }
    return allFieldsValid;
  }

  public void checkHouseKeyExist(String houseKeyToCheck) {
    String encryptHouseKey = EncryptionUtils.encrypt(houseKeyToCheck);
    Log.d("aaa", encryptHouseKey);
    housesRef.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        String emailHost = null;
        boolean isExist = false;
        for (DataSnapshot houseSnapshot : snapshot.getChildren()) {
          String houseKey = houseSnapshot.child("houseKey").getValue(String.class);
          if (houseKey.equals(encryptHouseKey)) {
            houseId = houseSnapshot.getKey();
            emailHost = houseSnapshot.child("emailHost").getValue(String.class);
            String email = edtEmail.getText().toString();
            isExist = true;

            role = emailHost.equals(email) ? "host" : "member";
            break;
          }
        }
        if (!isExist) {
          edtHouseKey.setError("House key does not exist!");
          isValidForm[3] = false;
        } else {
          isValidForm[3] = true;
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    });
  }

  public void checkRegisteredAccount(String emailToCheck, String houseKeyToCheck, String fullName, String telephone, String password) {
    String encryptHouseKey = EncryptionUtils.encrypt(houseKeyToCheck);

    housesRef.orderByChild("houseKey").equalTo(encryptHouseKey).addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
          for (DataSnapshot houseSnapshot : snapshot.getChildren()) {
            String houseId = houseSnapshot.getKey();
            Log.d("aaa", "houseRefs");
            usersRef.orderByChild("email").equalTo(emailToCheck).addListenerForSingleValueEvent(new ValueEventListener() {
              @Override
              public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                  for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    usersHousesRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                      @Override
                      public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                          for (DataSnapshot userHouseSnapshot : snapshot.getChildren()) {
                            String userIdToCheck = userHouseSnapshot.child("userId").getValue(String.class).toString();
                            String houseIdToCheck = userHouseSnapshot.child("houseId").getValue(String.class).toString();
                            if (userIdToCheck.equals(userId) && houseIdToCheck.equals(houseId)) {
                              Toast.makeText(SignupActivity.this, "Account already exists!", Toast.LENGTH_LONG).show();
                              edtHouseKey.setText("");
                              edtHouseKey.setError("Please re-enter another house key");
                              isValidForm[3] = false;
                            }
                          }
                          Log.d("aaa", "isValidForm[3]: " + isValidForm[3]);
                          if (isValidForm[3] == true) {
                            continueRegistration(userId, fullName, emailToCheck, telephone, password);
                          }
                        } else {
                          isValidForm[3] = true;
                          continueRegistration(userId, fullName, emailToCheck, telephone, password);
                        }
                      }

                      @Override
                      public void onCancelled(@NonNull DatabaseError error) {

                      }
                    });
                  }
                } else {
                  isValidForm[3] = true;
                  continueRegistration(null, fullName, emailToCheck, telephone, password);
                }
              }

              @Override
              public void onCancelled(DatabaseError databaseError) {
                // Handle database errors here
              }
            });
          }
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {

      }
    });

  }

  private void continueRegistration(String userId, String fullName, String email, String telephone, String password) {
    boolean allFieldsValid = true;
    for (boolean isValid : isValidForm) {
      Log.d("aaa", String.valueOf(isValid));
      if (!isValid) {
        allFieldsValid = false;
        break;
      }
    }
    if (allFieldsValid) {
      Log.d("aaa", isValidForm.toString());
      String encryptPass = EncryptionUtils.encrypt(password);

      if (userId == null) {
        userId = UUID.randomUUID().toString();
        writeNewUser(userId, fullName, email, telephone, encryptPass);
      }
      Log.d("aaa", "continueRegistration()");
      writeNewUserHouse(userId, fullName, email, telephone);
      moveScreen(SignupActivity.this, LoginActivity.class);
    }
  }

  public void writeNewUser(String userId, String name, String email, String telephone, String password) {
    name = Format.formatName(name);
    User user = new User(null, name, email, telephone, password);
    usersRef.child(userId).setValue(user);
  }

  public void writeNewUserHouse(String userId, String name, String email, String telephone) {
    UserHouse userHouse = new UserHouse(userId, houseId, role);
    usersHousesRef.child(UUID.randomUUID().toString()).setValue(userHouse);
    Toast.makeText(SignupActivity.this, "Successful account registration!", Toast.LENGTH_LONG).show();
    String password = edtPassword.getText().toString();
    User user = new User(userId, name, email, telephone, password);
    SharedUser.setUser(user);
  }

  public void moveScreen(Activity currentScreen, Class<? extends Activity> nextScreenClass) {
    Intent intent = new Intent(currentScreen, nextScreenClass);
    startActivity(intent);
  }

  private void clearFocusFromInputField() {
    View currentFocus = getCurrentFocus();
    if (currentFocus instanceof EditText) {
      currentFocus.clearFocus();
    }
  }
}