import { combineReducers } from '@reduxjs/toolkit';
import loader from 'redux/reducers/loader/loaderSlice';
import schemas from 'redux/reducers/schemas/schemasSlice';

export default combineReducers({
  loader,
  schemas,
});
