import React from 'react';

interface Props {
  serviceName: string;
}

function ServiceImage({ serviceName }: Props) {
  return <img src="images/serviceImage.png" alt={serviceName} />;
}

export default ServiceImage;
