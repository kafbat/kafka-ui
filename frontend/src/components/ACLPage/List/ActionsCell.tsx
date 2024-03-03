import { DropdownItem } from 'components/common/Dropdown';
import Dropdown from 'components/common/Dropdown/Dropdown';
import React from 'react';

type Props = {
  onDelete: () => void;
  onEdit: () => void;
};
const ActionsCell: React.FC<Props> = ({ onDelete, onEdit }) => {
  return (
    <Dropdown disabled={false}>
      <DropdownItem onClick={onEdit}>Edit ACL</DropdownItem>
      <DropdownItem danger onClick={onDelete}>
        Delete ACL
      </DropdownItem>
    </Dropdown>
  );
};

export default ActionsCell;
