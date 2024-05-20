import React from 'react';

import Footer from './Footer/Footer';
import Header from './Header/Header';
import SignIn from './SignIn/SignIn';
import * as S from './AuthPage.styled';

function AuthPage() {
  return (
    <S.AuthPageStyled>
      <Header />
      <SignIn type="service" />
      <Footer />
    </S.AuthPageStyled>
  );
}

export default AuthPage;
