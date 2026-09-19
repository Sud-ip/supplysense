import { useDispatch, useSelector } from 'react-redux'
import type { RootState, AppDispatch } from './store'

// Typed versions of the plain react-redux hooks - every component should
// import these, not the untyped originals, so useSelector/useDispatch
// know the shape of our state without repeating generics everywhere.
export const useAppDispatch = () => useDispatch<AppDispatch>()
export const useAppSelector = useSelector.withTypes<RootState>()
