import styled, { css } from 'styled-components';

export const Wrapper = styled.div`
  padding: 0;
  background: transparent;
  margin-bottom: 16px !important;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
`;

export const IndicatorWrapper = styled.div`
  background-color: ${({ theme }) => theme.surface.panel};
  border: 1px solid ${({ theme }) => theme.surface.border};
  border-radius: 8px;
  height: 82px;
  width: fit-content;
  min-width: 168px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 14px 16px;
  box-shadow: ${({ theme }) => theme.surface.shadow};
  flex-grow: 1;
  color: ${({ theme }) => theme.default.color.normal};

  & > div > span {
    font-size: 24px;
    font-weight: 700;
    line-height: 30px;
    letter-spacing: 0;
  }
`;

export const IndicatorTitle = styled.div`
  font-weight: 600;
  font-size: 12px;
  color: ${({ theme }) => theme.metrics.indicator.titleColor};
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
`;

export const IndicatorsWrapper = styled.div`
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  border-radius: 8px;
  overflow: visible;
  color: ${({ theme }) => theme.metrics.wrapper};
`;

export const SectionTitle = styled.h5`
  font-weight: 500;
  margin: 0 0 0.5rem;
  font-size: 100%;
  color: ${({ theme }) => theme.metrics.sectionTitle};
`;

export const LightText = styled.span`
  color: ${({ theme }) => theme.metrics.indicator.lightTextColor};
  font-size: 14px;
`;

export const RedText = styled.span`
  color: ${({ theme }) => theme.metrics.indicator.warningTextColor};
  font-size: 14px;
`;

export const CircularAlertWrapper = styled.svg.attrs({
  role: 'svg',
  viewBox: '0 0 4 4',
  xmlns: 'http://www.w3.org/2000/svg',
})`
  grid-area: status;
  fill: none;
  width: 4px;
  height: 4px;
`;

export const CircularAlert = styled.circle.attrs({
  role: 'circle',
  cx: 2,
  cy: 2,
  r: 2,
})<{
  $type: 'error' | 'success' | 'warning' | 'info';
}>(
  ({ theme, $type }) => css`
    fill: ${theme.circularAlert.color[$type]};
  `
);
